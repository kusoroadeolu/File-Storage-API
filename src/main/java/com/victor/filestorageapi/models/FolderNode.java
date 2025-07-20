package com.victor.filestorageapi.models;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class FolderNode {
    private String fullPath;
    private String name;
    private Map<String, FolderNode> children = new HashMap<>();

    public FolderNode(String fullPath){
        if(fullPath == null || fullPath.isEmpty()){
            this.fullPath = "/";
            this.name = "/";
        }else {
            String normalizedPath = fullPath.replaceAll("//+", "");

            if(!normalizedPath.startsWith("/")){
                normalizedPath = "/" + normalizedPath;
            }

            if(normalizedPath.equals("/")){
                this.fullPath = "/";
                this.name = "/";
            }

            int lastSlashIndex = normalizedPath.lastIndexOf("/");
            this.name = normalizedPath.substring(lastSlashIndex + 1);
            this.fullPath = normalizedPath;
        }
    }

    public FolderNode addChild(String childName, String childFullPath){
        return this.children.computeIfAbsent(childName, fNode -> new FolderNode(childFullPath));
    }

    public List<String> getFlattenedTree() {
        List<String> list = new ArrayList<>();
        return flattenTree(list, 0); // Start the printing with no initial indentation
    }

    private List<String> flattenTree(List<String> list, int indentLevel) {
        //Add each folder path everytime print tree is called
        list.add(this.getFullPath());
        //Recursively call printTree for each child
        for (FolderNode child : children.values()) {
             child.flattenTree(list, indentLevel + 1); // Increase indent level for children
        }
        return list;
    }
}
