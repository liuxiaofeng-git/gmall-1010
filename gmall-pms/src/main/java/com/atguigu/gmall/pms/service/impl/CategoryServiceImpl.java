package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.vo.CategoryVo;
import com.atguigu.gmall.pms.vo.ItemCategoryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.CategoryMapper;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<CategoryEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<CategoryEntity> queryCategoryByParentId(Long parentId) {
        QueryWrapper<CategoryEntity> wrapper = new QueryWrapper<>();
        if (parentId != -1) {
            wrapper.eq("parent_id", parentId);
        }
        List<CategoryEntity> categoryEntityList = baseMapper.selectList(wrapper);
        return categoryEntityList;
    }

    @Override
    public List<CategoryVo> queryCategoryVoByParentId(Long pid) {

        return this.categoryMapper.queryCategoryVoByParentId(pid);
    }

    @Override
    public List<ItemCategoryVo> queryCategoriesByCid3(Long cid3) {
        //根据三级分类id查询出三级分类对象
        CategoryEntity categoryEntity3 = this.categoryMapper.selectById(cid3);
        ItemCategoryVo itemCategoryVo3 = new ItemCategoryVo();
        itemCategoryVo3.setCategoryId(categoryEntity3.getId());
        itemCategoryVo3.setCategoryName(categoryEntity3.getName());

        //根据二级分类id查询出二级分类对象
        CategoryEntity categoryEntity2 = this.categoryMapper.selectById(categoryEntity3.getParentId());
        ItemCategoryVo itemCategoryVo2 = new ItemCategoryVo();
        itemCategoryVo2.setCategoryId(categoryEntity2.getId());
        itemCategoryVo2.setCategoryName(categoryEntity2.getName());

        //根据一级分类id查询出一级分类对象
        CategoryEntity categoryEntity1 = this.categoryMapper.selectById(categoryEntity2.getParentId());
        ItemCategoryVo itemCategoryVo1 = new ItemCategoryVo();
        itemCategoryVo1.setCategoryId(categoryEntity1.getId());
        itemCategoryVo1.setCategoryName(categoryEntity1.getName());

        ArrayList<ItemCategoryVo> itemCategoryVoArrayList = new ArrayList<>();
        itemCategoryVoArrayList.add(itemCategoryVo1);
        itemCategoryVoArrayList.add(itemCategoryVo2);
        itemCategoryVoArrayList.add(itemCategoryVo3);
        return itemCategoryVoArrayList;
    }

}