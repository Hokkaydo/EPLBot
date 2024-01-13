package com.github.hokkaydo.eplbot.module.rss;

import java.util.Date;
import java.util.Objects;

public record Article(String title, String description, String link, String imgURL, Date publishedDate) {

    @Override
    public String toString() {
        return STR."Article{title='\{title}\{'\''}, description='\{description}\{'\''}, link='\{link}\{'\''}, imgURL='\{imgURL}\{'\''}, publishedDate='\{publishedDate}\{'}'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Article article = (Article) o;

        if (!title.equals(article.title)) return false;
        if (!description.equals(article.description)) return false;
        if (!link.equals(article.link)) return false;
        if (!Objects.equals(imgURL, article.imgURL)) return false;
        return publishedDate.equals(article.publishedDate);
    }

    @Override
    public int hashCode() {
        int result = title.hashCode();
        result = 31 * result + link.hashCode();
        result = 31 * result + link.hashCode();
        result = 31 * result + (imgURL != null ? imgURL.hashCode() : 0);
        result = 31 * result + publishedDate.hashCode();
        return result;
    }

}