package co.istad.rentiq_api.features.category;



import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> listCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable Integer id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @GetMapping("/categories/{id}/children")
    public ResponseEntity<List<CategoryResponse>> getChildren(@PathVariable Integer id) {
        return ResponseEntity.ok(categoryService.getChildCategories(id));
    }

    @GetMapping("/categories/{id}/items")
    public ResponseEntity<List<Map<String, Object>>> getItems(@PathVariable Integer id) {
        return ResponseEntity.ok(categoryService.getItemsInCategory(id));
    }

    @PostMapping("/admin/categories")
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request));
    }

    @PatchMapping("/admin/categories/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Integer id, @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @DeleteMapping("/admin/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Integer id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/admin/categories/{id}/commission-rate")
    public ResponseEntity<CategoryResponse> updateCommissionRate(@PathVariable Integer id,
                                                                 @RequestBody Map<String, Double> payload) {
        return ResponseEntity.ok(categoryService.updateCommissionRate(id, payload.get("commissionRate")));
    }

    @PatchMapping("/admin/categories/{id}/status")
    public ResponseEntity<CategoryResponse> updateStatus(@PathVariable Integer id, @RequestBody Map<String, Boolean> payload) {
        return ResponseEntity.ok(categoryService.updateStatus(id, payload.get("active")));
    }
}