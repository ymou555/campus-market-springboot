package org.example.campusmarket.entity;

import lombok.Data;

import java.util.List;

@Data
public class CategoryTree {
    private Integer value;
    private String label;
    private List<CategoryTree> children;
    
    public CategoryTree() {
    }
    
    public CategoryTree(Integer value, String label) {
        this.value = value;
        this.label = label;
    }
}
