package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {
    void save(DishDTO dishDTO);

    PageResult queryPage(DishPageQueryDTO dishPageQueryDTO);

    void delete(String ids);

    DishVO getById(Long id);

    void updateWithFlavor(DishDTO dishDTO);

    void setStatus(Integer status, Integer id);

    List<Dish> getByCategoryId(Long categoryId);

    List<DishVO> getByCategoryIdWithFlavor(Long categoryId);


}
