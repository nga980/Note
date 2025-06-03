package com.example.notes.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index; // Thêm import này
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "notes", indices = {@Index(value = {"timestamp"})}) // Thêm index cho timestamp
public class Note implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "date_time") // Chuỗi ngày tháng để hiển thị
    private String dateTime;

    @ColumnInfo(name = "subtitle")
    private String subtitle;

    @ColumnInfo(name = "note_text")
    private String noteText;

    @ColumnInfo(name = "image_path")
    private String imagePath;

    @ColumnInfo(name = "color")
    private String color;

    @ColumnInfo(name = "web_link")
    private String webLink;

    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    private boolean isDeleted;

    @ColumnInfo(name = "deletion_time", defaultValue = "0")
    private long deletionTime;

    @ColumnInfo(name = "drawing_path")
    private String drawingPath;

    // Trường mới để lưu dấu thời gian (timestamp) cho việc sắp xếp
    @ColumnInfo(name = "timestamp")
    private long timestamp;


    public Note() {
        this.isDeleted = false;
        this.deletionTime = 0;
        this.timestamp = System.currentTimeMillis(); // Giá trị mặc định khi tạo mới
    }

    // ... (các getter và setter hiện có) ...

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getNoteText() {
        return noteText;
    }

    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getWebLink() {
        return webLink;
    }

    public void setWebLink(String webLink) {
        this.webLink = webLink;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public long getDeletionTime() {
        return deletionTime;
    }

    public void setDeletionTime(long deletionTime) {
        this.deletionTime = deletionTime;
    }

    public String getDrawingPath() {
        return drawingPath;
    }

    public void setDrawingPath(String drawingPath) {
        this.drawingPath = drawingPath;
    }

    // Getter và Setter cho timestamp
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return title + " : " + dateTime;
    }
}