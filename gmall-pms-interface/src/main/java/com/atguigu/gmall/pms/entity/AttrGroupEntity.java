package com.atguigu.gmall.pms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 属性分组
 * 
 * @author rookie
 * @email surookieqi@163.com
 * @date 2021-04-02 12:45:47
 */
@Data
@TableName("pms_attr_group")
public class AttrGroupEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 分组id
	 */
	@TableId
	private Long id;
	/**
	 * 组名
	 */
	private String name;
	/**
	 * 排序
	 */
	private Integer sort;
	/**
	 * 组图标
	 */
	private String icon;
	/**
	 * 所属分类id
	 */
	private Long categoryId;
	/**
	 * 备注
	 */
	private String remark;

	/**
	 *根据说明文档的信息添加一个新的属性，用于封装group表属性对应的属性表信息
	 *同时添加注解@TableField(exist = false)说明这个字段非表中字段。其他默认都有该注解，且exist的默认值为true(有该字段)
	 */
	@TableField(exist = false)
	private List<AttrEntity> attrEntities;
}
