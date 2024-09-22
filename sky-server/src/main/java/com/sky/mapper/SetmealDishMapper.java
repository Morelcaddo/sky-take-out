package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    //根据菜品id查询套餐id
    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);


    @Select("select * from setmeal_dish where setmeal_id = #{setmealId}")
    List<SetmealDish> getSetmealDishesByDishId(Long setmealId);

    @Select("select dish_id from setmeal_dish where setmeal_id = #{setmealId}")
    List<Long> getDishIdsBySetmealId(Long setmealId);

    void insertBatch(List<SetmealDish> setmealDishes);

    void deleteBySetmealId(Long setmealId);

    void deleteBySetmealIds(List<String> setmealIds);
}
