# PocketMind

PocketMind là một ứng dụng quản lý tài chính cá nhân nhẹ và hiện đại trên Android. Được xây dựng theo tiêu chuẩn kiến trúc Android năm 2026, ứng dụng sử dụng giao diện Material Design 3 trực quan, sắc nét và sạch sẽ.

## Tính năng

- **Bảng điều khiển Trang chủ**: Cung cấp cái nhìn tổng quan về chi tiêu hàng tháng, so sánh sự thay đổi (+/-) so với tháng trước và trực quan hóa chi tiêu thông qua Biểu đồ tròn. Truy cập nhanh vào danh sách giao dịch gần nhất.
- **Thêm giao dịch**: Ghi chép thu nhập hoặc chi tiêu nhanh chóng. Hỗ trợ phân loại, ghi chú, tải lên hình ảnh (hóa đơn) và tính năng nhập liệu bằng giọng nói (Voice-to-text).
- **Báo cáo**: Phân tích tình hình tài chính theo thời gian bằng Biểu đồ cột. Lọc dữ liệu theo tháng và tìm ra các danh mục bạn chi tiêu nhiều nhất.
- **Thông tin cá nhân**: Quản lý tài khoản, bật/tắt Chế độ tối (Dark Mode) và lưu/xuất dữ liệu cá nhân ra file CSV.
- **Xác thực**: Đăng nhập và đăng ký an toàn bằng email tùy chỉnh hoặc Đăng nhập qua Google (Google Sign-In).

## Công nghệ sử dụng

- **Ngôn ngữ**: Java
- **Kiến trúc**: Single Activity + Multiple Fragments (Navigation Component)
- **Giao diện (UI)**: ConstraintLayout & Material Components (M3)
- **Biểu đồ**: MPAndroidChart
- **Hệ thống Build**: Gradle (Kotlin DSL, `libs.versions.toml`)

## Hướng dẫn cài đặt

### Yêu cầu hệ thống

- Android Studio Koala (hoặc mới hơn)
- Android SDK 36
- Java 11+

### Cài đặt và Chạy

1. Clone repository về máy:
   ```bash
   git clone https://github.com/yourusername/PocketMind.git
   ```
2. Mở dự án bằng Android Studio.
3. Chờ Gradle đồng bộ (Sync) các file cấu hình.
4. Chạy dự án trên máy ảo (Emulator) hoặc thiết bị thật.

## Cấu trúc dự án

Dự án này tuân theo các nguyên tắc kiến trúc Android tiêu chuẩn:

- `ui/`: Chứa các Activity và Fragment, được nhóm theo từng tính năng (`auth`, `home`, v.v...).
- `utils/`: Chứa các class tiện ích như `AppLogger`.
- `res/layout/`: Toàn bộ file giao diện XML, sử dụng `ConstraintLayout`.
- `res/values/`: Chứa các file cấu hình giao diện trung tâm, bao gồm danh sách màu sắc và hỗ trợ đa ngôn ngữ (Tiếng Anh EN / Tiếng Việt VI).

## Đóng góp (Contributing)

1. Fork repository và tạo một nhánh (branch) mới.
2. Đảm bảo bạn **không** commit file `local.properties` hoặc các file rác của IDE (Tham khảo `.gitignore`).
3. Tạo một Pull Request mô tả chi tiết các thay đổi của bạn.

## Giấy phép (License)

Dự án này được cấp phép theo Giấy phép MIT - Vui lòng xem file LICENSE để biết chi tiết.
