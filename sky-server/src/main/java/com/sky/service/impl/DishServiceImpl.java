package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.message.Message;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Service
@Slf4j
@CacheConfig(cacheNames = "dishCathe")
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Override
    @Transactional
    public void save(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        dishMapper.insert(dish);

        long dishId = dish.getId();
        List<DishFlavor> list = dishDTO.getFlavors();

        if (list != null && !list.isEmpty()) {
            for (DishFlavor dishFlavor : list) {
                dishFlavor.setDishId(dishId);
            }
            dishFlavorMapper.insertBatch(list);
        }
    }

    @Override
    public PageResult queryPage(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    @Transactional
    public void delete(String ids) {
        String[] s = ids.split(",");
        List<Long> dishIds = new ArrayList<>();
        for (String string : s) {
            dishIds.add(Long.parseLong(string));
        }

        //判断菜品是否起售
        for (Long aLong : dishIds) {
            Dish dish = dishMapper.getById(aLong);
            System.out.println(dish.getStatus());
            if (Objects.equals(dish.getStatus(), StatusConstant.ENABLE)) {
                //当前菜品正在起售，不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        //判断当前菜品是否被套餐管理
        List<Long> setMealIds = setmealDishMapper.getSetmealIdsByDishIds(dishIds);
        if (setMealIds != null && !setMealIds.isEmpty()) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        //删除菜品表中的菜品数据
        dishMapper.deleteByIds(dishIds);
        dishFlavorMapper.deleteByDishIds(dishIds);

    }

    @Override
    @Transactional
    public DishVO getById(Long id) {
        Dish dish = dishMapper.getById(id);

        List<DishFlavor> list = dishFlavorMapper.getByDishId(id);

        DishVO dishVO = new DishVO();

        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(list);

        return dishVO;
    }

    @Override
    @Transactional
    @CacheEvict(key = "#dishDTO.categoryId")
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);
        dishFlavorMapper.deleteByDishId(dish.getId());

        List<DishFlavor> list = dishDTO.getFlavors();

        if (list != null && !list.isEmpty()) {
            for (DishFlavor dishFlavor : list) {
                dishFlavor.setDishId(dish.getId());
            }
            dishFlavorMapper.insertBatch(list);
        }
    }

    @Override
    @CacheEvict(allEntries = true)
    public void setStatus(Integer status, Integer id) {
        dishMapper.setStatus(status, id);
    }

    @Override
    @Transactional
    public List<Dish> getByCategoryId(Long categoryId) {
        return dishMapper.getByCategoryId(categoryId);
    }

    @Override
    @Transactional
    @Cacheable(key = "#categoryId")
    public List<DishVO> getByCategoryIdWithFlavor(Long categoryId) {
        List<DishVO> list = new ArrayList<>();
        List<Dish> dishes = dishMapper.getByCategoryId(categoryId);
        for (Dish dish : dishes) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(dish, dishVO);
            List<DishFlavor> dishFlavorList = dishFlavorMapper.getByDishId(dish.getId());
            dishVO.setFlavors(dishFlavorList);
            list.add(dishVO);
        }
        return list;
    }
}
