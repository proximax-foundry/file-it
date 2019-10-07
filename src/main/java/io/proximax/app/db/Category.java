package io.proximax.app.db;

/**
 *
 * @author thcao
 */
public class Category {

    public int id;
    public String category;
    public long createdDate;
    public String parent;

    public Category(String folder) {
        id = 0;
        category = folder;
        createdDate = 0;
        parent = "";
    }

    public Category(Category folder) {
        id = folder.id;
        category = folder.category;
        createdDate = folder.createdDate;
        parent = folder.parent;
    }

}
