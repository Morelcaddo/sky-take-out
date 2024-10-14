package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private DishMapper dishMapper;

    @Override
    @Transactional
    public void save(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();

        BeanUtils.copyProperties(setmealDTO, setmeal);

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        List<Long> list = new ArrayList<>();//存放dishId

        setmeal.setStatus(StatusConstant.DISABLE);
        setmealMapper.insert(setmeal);

        Long setmealId = setmeal.getId();

        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(setmealId);
            list.add(setmealDish.getDishId());
        }

        Long cnt = dishMapper.getStatusByDishIds(list);

        if (cnt != 0) {
            throw new SetmealEnableFailedException(MessageConstant.DISH_ON_SALE);
        }

        setmealDishMapper.insertBatch(setmealDishes);
    }

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());

        List<SetmealVO> list = setmealMapper.query(setmealPageQueryDTO);

        Page<SetmealVO> page = (Page<SetmealVO>) list;

        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "setmealCathe:*", allEntries = true)
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        System.out.println(setmeal.getId());
        setmealDishMapper.deleteBySetmealId(setmeal.getId());

        setmealMapper.update(setmeal);

        List<SetmealDish> list = setmealDTO.getSetmealDishes();
        for (SetmealDish setmealDish : list) {
            setmealDish.setSetmealId(setmeal.getId());
        }
        setmealDishMapper.insertBatch(list);

    }


    @Override
    @Transactional
    public SetmealVO getById(Long id) {
        Setmeal setmeal = setmealMapper.getById(id);
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishMapper.getSetmealDishesByDishId(id));
        return setmealVO;
    }

    @Override
    @Transactional
    public void deleteByIds(String ids) {
        List<String> allId = Arrays.asList(ids.split(","));

        if (setmealMapper.inspectStatus(allId) != 0) {
            throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ON_SALE);
        }

        setmealDishMapper.deleteBySetmealIds(allId);
        setmealMapper.deleteByIds(allId);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "setmealCathe:*", allEntries = true)
    public void startOrStopStatus(Integer status, Long id) {

        List<Long> dishIds = setmealDishMapper.getDishIdsBySetmealId(id);

        if (dishMapper.getStatusByDishIds(dishIds) != 0) {
            throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
        }

        setmealMapper.startOrStopStatus(status, id);

    }

    @Override
    @Cacheable(cacheNames = "setmealCathe:categoryCathe", key = "#categoryId")
    public List<Setmeal> getByCategoryId(Long categoryId) {
        return setmealMapper.getByCategoryId(categoryId);
    }

    @Override
    @Cacheable(cacheNames = "setmealCathe:dishCathe", key = "#id")
    public List<DishItemVO> getDishesById(Long id) {
        return setmealDishMapper.getDishesBySetmealId(id);
    }
}
