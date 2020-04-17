package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.List;
@Data
public class CategoryVo extends CategoryEntity {

    @TableField(exist = false)
    private List<CategoryEntity> subs;
}
