package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {

    void save(SetmealDTO setmealDTO);

    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    void update(SetmealDTO setmealDTO);

    SetmealVO getById(Long id);

    void deleteByIds(String ids);

    void startOrStopStatus(Integer status, Long id);

    List<Setmeal> getByCategoryId(Long categoryId);

    List<DishItemVO> getDishesById(Long id);
}
