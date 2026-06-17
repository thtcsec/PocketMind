# PocketMind

PocketMind là ứng dụng quản lý tài chính cá nhân nhẹ và hiện đại trên Android. Được xây dựng theo tiêu chuẩn kiến trúc Android năm 2026 với **Kotlin** và **Jetpack Compose**, giao diện Material Design 3 trực quan, sắc nét và sạch sẽ.

## Tính năng

- **Bảng điều khiển Trang chủ**: Tổng quan chi tiêu hàng tháng, biểu đồ tròn theo danh mục và danh sách giao dịch gần nhất.
- **Thêm giao dịch**: Ghi chép thu nhập/chi tiêu thủ công hoặc chat AI (hỗ trợ gửi ảnh hóa đơn).
- **Báo cáo**: Phân tích theo khoảng thời gian với biểu đồ cột/tròn, tổng thu/chi và danh mục hàng đầu.
- **Thông tin cá nhân**: Quản lý tài khoản, cài đặt giao diện/ngôn ngữ/tiền tệ, xóa tài khoản.
- **Xác thực**: Đăng nhập/đăng ký email hoặc Google Sign-In qua Firebase Auth.
- **AI & Admin**: Cấu hình gói AI, thanh toán thủ công, bảng quản trị người dùng (role admin).

## Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | **Kotlin** |
| UI | **Jetpack Compose** + Material 3 |
| Kiến trúc | Single Activity + Navigation Compose + ViewModel |
| State | StateFlow + `collectAsStateWithLifecycle` |
| Backend | Firebase (Auth, Firestore, Storage, Analytics) |
| Ảnh | Coil 3 |
| Build | Gradle Kotlin DSL, `libs.versions.toml`, AGP 9 |

## Hướng dẫn cài đặt

### Yêu cầu hệ thống

- Android Studio Koala trở lên
- Android SDK 36
- JDK 11+
- File `google-services.json` (đặt trong `app/`, không commit)

### Cài đặt và chạy

```bash
git clone https://github.com/yourusername/PocketMind.git
cd PocketMind
```

1. Thêm `app/google-services.json` từ Firebase Console.
2. Mở dự án bằng Android Studio.
3. Gradle Sync.
4. Chạy trên emulator hoặc thiết bị thật.

```bash
./gradlew :app:assembleDebug
```

## Cấu trúc dự án

```
app/src/main/kotlin/com/tuhoang/pocketmind/
├── MainActivity.kt              # Entry Compose
├── PocketMindApp.kt             # Application + theme/config bootstrap
├── data/models/                 # Firestore POJOs
├── utils/                       # PrefsManager, CurrencyUtils, ...
└── ui/
    ├── theme/                   # Material 3 theme
    ├── navigation/              # NavHost & routes
    ├── main/                    # Bottom navigation shell
    ├── home/                    # Dashboard
    ├── chat/                    # Manual entry + AI chat
    ├── report/                  # Analytics
    ├── profile/                 # Account hub & edit
    ├── auth/                    # Login & register
    ├── settings/                # App settings & AI plans
    └── admin/                   # Admin dashboard
```

## Đóng góp

1. Fork repository và tạo branch mới.
2. Không commit `local.properties`, `google-services.json` hoặc file IDE.
3. Tạo Pull Request mô tả chi tiết thay đổi.

## Giấy phép

Dự án này được cấp phép theo Giấy phép MIT — xem file LICENSE để biết chi tiết.
