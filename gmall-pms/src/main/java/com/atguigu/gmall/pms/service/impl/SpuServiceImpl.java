package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.mapper.SpuDescMapper;
import com.atguigu.gmall.pms.service.*;
import com.atguigu.gmall.pms.vo.SkuVo;
import com.atguigu.gmall.pms.vo.SpuAttrValueVo;
import com.atguigu.gmall.pms.vo.SpuVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SpuMapper;
import org.springframework.util.CollectionUtils;


@Service("spuService")
public class SpuServiceImpl extends ServiceImpl<SpuMapper, SpuEntity> implements SpuService {

    @Autowired
    private SpuDescMapper descMapper;

    @Autowired
    private SpuAttrValueService spuAttrValueService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SkuAttrValueService skuAttrValueService;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SpuEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public PageResultVo queryCategoryByCidAndPage(PageParamVo paramVo, Long cid) {
        QueryWrapper<SpuEntity> wrapper = new QueryWrapper<>();
        //?????????0??????category_id??????????????????????????????0???????????????????????????????????????????????????0????????????????????????
        if (cid != 0) {
            wrapper.eq("category_id", cid);
        }
        //??????????????????key
        String key = paramVo.getKey();

        if (StringUtils.isNotBlank(key)) {
            //??????select *from attr where category_id=? and (id=? or name like '%?%')
            wrapper.and(t -> t.eq("id", key).or().like("name", key));
        }

        IPage<SpuEntity> page = this.page(
                paramVo.getPage(),
                wrapper
        );

        return new PageResultVo(page);
    }

    @GlobalTransactional
    @Override
    public void bigSave(SpuVo spuVo) {
        QueryWrapper<SpuVo> wrapper = new QueryWrapper<>();
        //1.??????spu????????????


        //1.1??????pms_spu?????????
        //????????????
        spuVo.setCreateTime(new Date());
        spuVo.setUpdateTime(spuVo.getCreateTime());
        //??????json?????????pms_spu???????????????
        this.save(spuVo);


        //??????pms_spu???id????????????????????????spu_id???
        Long spuId = spuVo.getId();
        //1.2??????pms_spu_attr_value?????????
        List<SpuAttrValueVo> baseAttrs = spuVo.getBaseAttrs();

        if (!CollectionUtils.isEmpty(baseAttrs)) {
            /*List<SpuAttrValueEntity> spuAttrValueEntities = baseAttrs.stream().map(SpuAttrValueVo -> {
                SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
                BeanUtils.copyProperties(SpuAttrValueVo, spuAttrValueEntity);
                spuAttrValueEntity.setSpuId(spuId);
                return spuAttrValueEntity;
            }).collect(Collectors.toList());
            this.spuAttrValueService.saveBatch(spuAttrValueEntities);
            */

            this.spuAttrValueService.saveBatch(baseAttrs.stream().map(SpuAttrValueVo -> {
                SpuAttrValueEntity spuAttrValueEntity = new SpuAttrValueEntity();
                BeanUtils.copyProperties(SpuAttrValueVo, spuAttrValueEntity);
                spuAttrValueEntity.setSpuId(spuId);
                return spuAttrValueEntity;
            }).collect(Collectors.toList()));
        }
        //1.3??????pms_desc?????????
        List<String> spuImages = spuVo.getSpuImages();
        if (!CollectionUtils.isEmpty(spuImages)) {
            SpuDescEntity spuDescEntity = new SpuDescEntity();
            spuDescEntity.setSpuId(spuId);
            spuDescEntity.setDecript(StringUtils.join(spuImages, ","));
            this.descMapper.insert(spuDescEntity);
        }
        //2.??????sku????????????
        //2.1??????pms_sku?????????
        List<SkuVo> skus = spuVo.getSkus();
        if (CollectionUtils.isEmpty(skus)) {
            return;
        }
        skus.forEach(sku -> {
            sku.setSpuId(spuId);

            sku.setCategoryId(spuVo.getCategoryId());

            sku.setBrandId(spuVo.getBrandId());

            List<String> images = sku.getImages();

            if (!CollectionUtils.isEmpty(images)) {
                sku.setDefaultImage(
                        StringUtils.isNotBlank(sku.getDefaultImage())
                                ? sku.getDefaultImage() : sku.getImages().get(0));
            }

            this.skuMapper.insert(sku);
            Long skuId = sku.getId();
            //2.2??????pms_sku_attr_value?????????

            List<SkuAttrValueEntity> saleAttrs = sku.getSaleAttrs();
            if(!CollectionUtils.isEmpty(saleAttrs)){
                saleAttrs.forEach(skuAttrValueEntity -> skuAttrValueEntity.setSkuId(skuId));
                this.skuAttrValueService.saveBatch(saleAttrs);
            }

            //2.3??????pms_images?????????
            if(!CollectionUtils.isEmpty(images)){
                this.skuImagesService.saveBatch(images.stream().map(image ->{
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setUrl(image);
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setDefaultStatus(
                            StringUtils.equals(sku.getDefaultImage(), image) ? 0 : 1
                    );
                    return skuImagesEntity;
                }).collect(Collectors.toList()));
            }
            // ????????????????????????elasticsearch????????????
            this.rabbitTemplate.convertAndSend("PMS_ITEM_EXCHANGE", "item.insert", spuId);
            //3.??????????????????????????????

            SkuSaleVo saleVo = new SkuSaleVo();
            BeanUtils.copyProperties(sku,saleVo);
            saleVo.setSkuId(skuId);
            this.smsClient.saveSales(saleVo);
//            int i = 10 / 0;
        });




    }

}