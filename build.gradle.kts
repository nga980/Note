// Top-level build file (ví dụ: Notes/build.gradle.kts)
// nơi bạn có thể thêm các tùy chọn cấu hình chung cho tất cả các project con/module.
plugins {
    id("com.android.application") version "8.10.1" apply false // Phiên bản AGP của bạn
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false // THÊM DÒNG NÀY
    // Nếu bạn có ý định sử dụng KSP (thay cho kapt), bạn cũng có thể khai báo nó ở đây:
    // id("com.google.devtools.ksp") version "1.9.23-1.0.19" apply false // Ví dụ phiên bản KSP
}