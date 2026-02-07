package com.appcompras.planning;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class MealPlanController {

    private final MealPlanService mealPlanService;

    public MealPlanController(MealPlanService mealPlanService) {
        this.mealPlanService = mealPlanService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MealPlanResponse createPlan(@Valid @RequestBody CreateMealPlanRequest request) {
        MealPlan plan = mealPlanService.create(request);
        return MealPlanResponse.from(plan);
    }

    @GetMapping("/{id}")
    public MealPlanResponse getPlanById(@PathVariable String id) {
        MealPlan plan = mealPlanService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
        return MealPlanResponse.from(plan);
    }

    @GetMapping
    public List<MealPlanResponse> getPlans() {
        return mealPlanService.findAll().stream()
                .map(MealPlanResponse::from)
                .toList();
    }

    @PutMapping("/{id}")
    public MealPlanResponse updatePlan(@PathVariable String id, @Valid @RequestBody CreateMealPlanRequest request) {
        MealPlan plan = mealPlanService.update(id, request)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
        return MealPlanResponse.from(plan);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlan(@PathVariable String id) {
        boolean deleted = mealPlanService.deleteById(id);
        if (!deleted) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found");
        }
    }
}
