package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
public class DishController {

    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping
    @ApiOperation("新增菜品")
    public Result<String> save(@RequestBody DishDTO dishDTO) {
        //cleanCathe("dish_*");
        dishService.save(dishDTO);
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> queryPage(DishPageQueryDTO dishPageQueryDTO) {
        return Result.success(dishService.queryPage(dishPageQueryDTO));
    }

    @DeleteMapping
    @ApiOperation("菜品删除")
    public Result<String> delete(String ids) {
        //cleanCathe("dish_*");
        dishService.delete(ids);
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id) {
        return Result.success(dishService.getById(id));
    }

    @PutMapping
    @ApiOperation("根据id修改菜品")
    public Result<String> update(@RequestBody DishDTO dishDTO) {
        cleanCathe("dish_" + dishDTO.getCategoryId().toString());
        dishService.updateWithFlavor(dishDTO);
        return Result.success();

    }

    @PostMapping("/status/{status}")
    @ApiOperation("菜品起售，停售")
    public Result<String> setStatus(@PathVariable Integer status, Integer id) {
        cleanCathe("dish_*");
        dishService.setStatus(status, id);
        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> getByCategoryId(Long categoryId) {
        return Result.success(dishService.getByCategoryId(categoryId));

    }

    private void cleanCathe(String pattern) {
        Set keys = redisTemplate.keys(pattern);
        if (keys != null) {
            redisTemplate.delete(keys);
        }
    }


}
