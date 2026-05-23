# Báo cáo phần Frontend Chat E2EE

## 1. Phạm vi công việc

Phần này tập trung vào nhiệm vụ **Frontend - Realtime Chat + E2EE Message**.

Mục tiêu chính:

- Hiển thị danh sách cuộc trò chuyện của user đã đăng nhập.
- Tạo cuộc trò chuyện 1-1 với user khác.
- Sinh `conversationKey` ở client.
- Mã hóa `conversationKey` bằng public key của các participant.
- Lấy và giải mã `encryptedConversationKey` khi mở chat.
- Mã hóa tin nhắn trước khi gửi lên server.
- Giải mã tin nhắn khi đọc từ server.
- Server chỉ nhận và lưu dữ liệu đã mã hóa, không xử lý plaintext.

Kiến trúc hiện tại đang dùng mô hình `conversationKey` theo `conversationId` và `keyVersion`, chưa phải mô hình Double Ratchet với `rootKey`, `chainKey`, `messageKey` cho từng tin nhắn.

## 2. Các giao diện đã làm

### 2.1. Màn danh sách cuộc trò chuyện

Code chính:

- `app/src/main/java/com/example/secapp/ui/screen/chat/ConversationListScreen.kt`

Mục đích:

- Là màn hình chính sau khi user đăng nhập và nhập PIN thành công.
- Hiển thị các conversation mà user đang tham gia.
- Cho phép user mở một conversation để vào màn chat detail.
- Cho phép user tạo conversation mới.
- Cho phép user đăng xuất.

Chức năng đã có:

- Gọi API lấy danh sách conversation.
- Hiển thị trạng thái loading khi đang tải dữ liệu.
- Hiển thị lỗi nếu API fail.
- Hiển thị trạng thái rỗng nếu chưa có conversation.
- Có nút `Thử lại` để load lại danh sách.
- Có nút `+` để tạo chat mới.
- Có nút logout.

Luồng hoạt động:

```text
ConversationListScreen
-> ChatRepository.listConversations()
-> ConversationService.listConversations()
-> GET /api/v1/conversations
-> Hiển thị ConversationItem lên UI
```

### 2.2. Màn tạo cuộc trò chuyện mới

Code chính:

- `app/src/main/java/com/example/secapp/ui/screen/chat/CreateConversationScreen.kt`

Mục đích:

- Cho phép user tìm người dùng khác bằng username/email.
- Cho phép chọn user để tạo cuộc trò chuyện trực tiếp 1-1.

Chức năng đã có:

- Ô nhập keyword để tìm user.
- Nút tìm kiếm.
- Hiển thị danh sách user tìm được.
- Bấm vào user để tạo conversation.
- Hiển thị loading khi đang search hoặc đang tạo conversation.
- Hiển thị lỗi nếu search/create fail.
- Sau khi tạo conversation thành công thì chuyển sang màn chat detail.

Luồng hoạt động:

```text
CreateConversationScreen
-> ChatRepository.searchUsers(keyword)
-> UserService.searchUsers(keyword)
-> GET /api/v1/users/search
-> User chọn người nhận
-> ChatRepository.createDirectConversation(user)
-> Sinh conversationKey
-> Mã hóa conversationKey cho các recipient
-> ConversationService.createConversation()
-> POST /api/v1/conversations
```

### 2.3. Màn chi tiết chat

Code chính:

- `app/src/main/java/com/example/secapp/ui/screen/chat/ChatDetailScreen.kt`

Mục đích:

- Hiển thị nội dung tin nhắn trong một conversation.
- Cho phép user gửi tin nhắn mới.
- Tin nhắn hiển thị trên UI là plaintext sau khi client giải mã.
- Dữ liệu gửi lên backend là ciphertext.

Chức năng đã có:

- Lấy `encryptedConversationKey` của conversation.
- Dùng private key đã unlock bằng PIN để giải mã ra `conversationKey`.
- Lấy danh sách message từ backend.
- Giải mã `cipherData` bằng `conversationKey`.
- Hiển thị message sau khi giải mã.
- Mã hóa message mới bằng AES-GCM trước khi gửi.
- Gửi message đã mã hóa lên backend.
- Có nút `Tải lại` để reload message.
- Có xử lý lỗi nếu không lấy được key hoặc không gọi được API.

Luồng đọc tin nhắn:

```text
ChatDetailScreen
-> ChatRepository.getConversationKey(conversationId)
-> ConversationService.getMyEncryptedKey(conversationId)
-> RSA decrypt encryptedConversationKey bằng masterPrivateKey/sessionPrivateKey
-> MessageService.getMessages(conversationId)
-> AES-GCM decrypt từng message
-> Hiển thị plaintext lên UI
```

Luồng gửi tin nhắn:

```text
User nhập message
-> ChatRepository.sendMessage()
-> AES-GCM encrypt plaintext bằng conversationKey
-> MessageService.sendMessage()
-> POST /api/v1/messages
-> Server lưu cipherData, iv, aad, keyVersion
```

## 3. Các phần xử lý đã làm

### 3.1. Authenticated API layer

Code chính:

- `app/src/main/java/com/example/secapp/data/remote/NetworkConfig.kt`
- `app/src/main/java/com/example/secapp/data/local/security/AuthSessionState.kt`

Đã làm:

- Tạo Retrofit client dùng chung cho API.
- Tạo OkHttp interceptor tự động gắn `Authorization: Bearer <accessToken>`.
- Lấy token từ `AuthSessionState` trước, nếu không có thì fallback sang `SecureStorage`.
- Cấu hình base URL backend online mới: `https://secapi-ibir.onrender.com/`.
- Cấu hình WebSocket URL: `wss://secapi-ibir.onrender.com/ws/messages`.

Vai trò:

- Giúp các service chat gọi backend mà không phải tự truyền token ở từng màn hình.
- Là cầu nối để phần chat dùng được token từ luồng Auth/PIN.

### 3.2. UserService

Code chính:

- `app/src/main/java/com/example/secapp/data/remote/UserService.kt`

Đã làm:

- Gọi API tìm kiếm user.
- Gọi API lấy public key/session key của user.

Vai trò:

- Dùng khi tạo conversation mới.
- FE cần public key của người nhận để mã hóa `conversationKey`.

### 3.3. ConversationService

Code chính:

- `app/src/main/java/com/example/secapp/data/remote/ConversationService.kt`

Đã làm:

- Gọi API lấy danh sách conversation.
- Gọi API tạo conversation mới.
- Gọi API lấy encrypted key của user hiện tại trong conversation.

Vai trò:

- Quản lý metadata của conversation.
- Không xử lý plaintext.
- Nhận/gửi `encryptedConversationKey`.

### 3.4. MessageService

Code chính:

- `app/src/main/java/com/example/secapp/data/remote/MessageService.kt`

Đã làm:

- Gọi API lấy messages theo conversation.
- Gọi API gửi message mới.

Vai trò:

- FE gửi `cipherData`, `iv`, `aad`, `keyVersion`.
- FE nhận dữ liệu đã mã hóa và tự giải mã ở client.

### 3.5. ChatRepository

Code chính:

- `app/src/main/java/com/example/secapp/data/repository/ChatRepository.kt`

Đã làm:

- Gom toàn bộ logic chat vào một lớp trung gian.
- UI không gọi service trực tiếp.
- Xử lý search user.
- Xử lý tạo conversation.
- Xử lý lấy và cache `conversationKey`.
- Xử lý load messages.
- Xử lý decrypt message.
- Xử lý encrypt và gửi message.

Vai trò:

- Là nơi nối giữa UI, API service và crypto.
- Giúp các màn hình chat đơn giản hơn.

### 3.6. ChatCrypto

Code chính:

- `app/src/main/java/com/example/secapp/data/local/security/ChatCrypto.kt`
- `app/src/main/java/com/example/secapp/data/local/security/CryptoHelper.kt`

Đã làm:

- Sinh random `conversationKey`.
- AES-GCM encrypt message.
- AES-GCM decrypt message.
- RSA encrypt `conversationKey` bằng public key của recipient.
- RSA decrypt `encryptedConversationKey` bằng private key của user.
- Encode/decode Base64 để gửi dữ liệu qua API.

Vai trò:

- Đảm bảo nội dung message chỉ được mã hóa/giải mã ở client.
- Backend chỉ lưu ciphertext.

### 3.7. Nơi giữ khóa đã unlock

Code chính:

- `app/src/main/java/com/example/secapp/data/local/security/AuthSessionState.kt`
- `app/src/main/java/com/example/secapp/data/local/security/SessionCryptoState.kt`
- `app/src/main/java/com/example/secapp/data/local/security/ConversationKeyCache.kt`
- `app/src/main/java/com/example/secapp/data/repository/AuthRepository.kt`

Đã làm:

- `AuthSessionState` giữ access token, refresh token, session key id trong RAM.
- `SessionCryptoState` giữ `sessionPrivateKey` và `masterPrivateKey` sau khi login/PIN unlock.
- `ConversationKeyCache` giữ `conversationKey` đã giải mã trong RAM.
- `AuthRepository` lưu token và khóa sau login/PIN để phần chat có thể dùng tiếp.

Vai trò:

- Đây là phần nối bắt buộc giữa phần Auth/PIN của Hà và phần Chat E2EE.
- Sau khi nhập PIN đúng, màn chat mới có `masterPrivateKey` để giải mã conversation key.

### 3.8. DTO cho chat

Code chính:

- `app/src/main/java/com/example/secapp/data/model/dto/ChatModels.kt`

Đã làm:

- Tạo model request/response cho user, conversation, encrypted key, message.
- Chuẩn hóa dữ liệu đi/đến giữa FE và BE.

Vai trò:

- Giúp Retrofit map JSON từ backend sang Kotlin object.
- Giúp repository xử lý dữ liệu rõ ràng hơn.

### 3.9. Navigation

Code chính:

- `app/src/main/java/com/example/secapp/ui/navigation/NavGraph.kt`
- `app/src/main/java/com/example/secapp/ui/navigation/Screen.kt`

Đã làm:

- Thêm route danh sách conversation.
- Thêm route tạo conversation.
- Thêm route chat detail.
- Sau khi PIN unlock thành công thì đi vào màn conversation list.

Vai trò:

- Nối luồng Auth/PIN sang luồng Chat.

## 4. Các mục công việc đã hoàn thành

### 4.1. Tạo authenticated API layer

Đã làm:

- Thêm token interceptor trong `NetworkConfig.kt`.
- Thêm `UserService`.
- Thêm `ConversationService`.
- Thêm `MessageService`.

Code ở:

- `app/src/main/java/com/example/secapp/data/remote/NetworkConfig.kt`
- `app/src/main/java/com/example/secapp/data/remote/UserService.kt`
- `app/src/main/java/com/example/secapp/data/remote/ConversationService.kt`
- `app/src/main/java/com/example/secapp/data/remote/MessageService.kt`

### 4.2. Bổ sung crypto cho chat

Đã làm:

- Sinh `conversationKey`.
- RSA encrypt/decrypt `conversationKey`.
- AES-GCM encrypt/decrypt message.

Code ở:

- `app/src/main/java/com/example/secapp/data/local/security/ChatCrypto.kt`
- `app/src/main/java/com/example/secapp/data/local/security/CryptoHelper.kt`

### 4.3. Tạo nơi giữ khóa đã unlock

Đã làm:

- Giữ token trong RAM.
- Giữ `sessionPrivateKey` trong RAM.
- Giữ `masterPrivateKey` trong RAM sau PIN unlock.
- Cache `conversationKey` theo conversation.

Code ở:

- `app/src/main/java/com/example/secapp/data/local/security/AuthSessionState.kt`
- `app/src/main/java/com/example/secapp/data/local/security/SessionCryptoState.kt`
- `app/src/main/java/com/example/secapp/data/local/security/ConversationKeyCache.kt`
- `app/src/main/java/com/example/secapp/data/repository/AuthRepository.kt`

### 4.4. Làm conversation list

Đã làm:

- Gọi API `GET /api/v1/conversations`.
- Hiển thị danh sách conversation.
- Cho phép mở chat detail.

Code ở:

- `app/src/main/java/com/example/secapp/ui/screen/chat/ConversationListScreen.kt`
- `app/src/main/java/com/example/secapp/data/repository/ChatRepository.kt`

### 4.5. Làm create conversation

Đã làm:

- Search user.
- Lấy public key/session key của user.
- Sinh `conversationKey`.
- Mã hóa `conversationKey` cho recipient.
- Gửi `encryptedKeys` lên backend.

Code ở:

- `app/src/main/java/com/example/secapp/ui/screen/chat/CreateConversationScreen.kt`
- `app/src/main/java/com/example/secapp/data/repository/ChatRepository.kt`

### 4.6. Làm chat detail

Đã làm:

- Lấy `encryptedConversationKey`.
- Giải mã ra `conversationKey`.
- Lấy messages.
- Decrypt messages.
- Encrypt và gửi message mới.

Code ở:

- `app/src/main/java/com/example/secapp/ui/screen/chat/ChatDetailScreen.kt`
- `app/src/main/java/com/example/secapp/data/repository/ChatRepository.kt`

### 4.7. WebSocket realtime

Đã làm:

- Tạo client WebSocket cơ bản.
- Có cấu hình endpoint WebSocket.
- Có phần nền để gửi/nhận realtime message.

Code ở:

- `app/src/main/java/com/example/secapp/data/remote/ChatRealtimeClient.kt`
- `app/src/main/java/com/example/secapp/ui/screen/chat/ChatDetailScreen.kt`

Trạng thái:

- Chưa xem là hoàn thành ổn định.
- REST chat đang là luồng chính cần chạy chắc trước.

## 5. Các phần chưa làm và cần làm tiếp

### 5.1. WebSocket realtime hoàn chỉnh

Chưa làm xong:

- Chưa test ổn định realtime giữa hai thiết bị/tài khoản.
- Chưa xử lý reconnect.
- Chưa xử lý token hết hạn khi WebSocket đang chạy.
- Chưa đồng bộ message realtime với REST fallback thật sạch.

Cần làm:

- Test gửi/nhận realtime bằng hai tài khoản.
- Chốt format payload WebSocket với backend.
- Parse message realtime rồi decrypt bằng `conversationKey`.
- Nếu mất kết nối thì fallback reload messages bằng REST.

### 5.2. Multi-session hoàn chỉnh

Chưa làm xong:

- Mới hỗ trợ logic tạo encrypted key cho master/session ở mức cơ bản.
- Chưa test kỹ trường hợp một user có nhiều thiết bị/session.
- Chưa có UI quản lý session.

Cần làm:

- Test cùng một user đăng nhập trên nhiều thiết bị.
- Đảm bảo mỗi session nhận được một `encryptedConversationKey` riêng.
- Khi session mới xuất hiện, cần có luồng bổ sung key cho session mới.

### 5.3. Key recovery UX

Chưa làm xong:

- Khi client không giải mã được `conversationKey`, UI mới báo lỗi.
- Chưa có luồng khôi phục rõ ràng cho user.

Cần làm:

- Nếu mất key thì yêu cầu nhập lại PIN.
- Nếu vẫn không khôi phục được thì báo rõ conversation không thể đọc trên thiết bị này.
- Có thể thêm luồng request key lại từ session/master key hợp lệ.

### 5.4. Key rotation

Chưa làm:

- Chưa xoay `conversationKey`.
- Chưa tạo `keyVersion` mới khi rotate key.
- Chưa xử lý message cũ dùng key cũ và message mới dùng key mới.

Cần làm:

- Khi rotate, tạo `conversationKey` mới.
- Gửi lại encrypted key cho các participant/session.
- Message mới dùng `keyVersion` mới.
- Message cũ vẫn decrypt bằng key cũ.

### 5.5. Message key từng tin nhắn / Double Ratchet

Chưa làm:

- Hiện tại nhiều message trong một conversation đang dùng chung `conversationKey`.
- Chưa có `rootKey`, `chainKey`, `messageKey`.
- Chưa có forward secrecy thật sự như Signal Double Ratchet.

Cần làm nếu nâng cấp bảo mật:

- Thiết kế `session state` riêng cho từng cặp user/device.
- Sinh `messageKey` riêng cho từng tin nhắn.
- Sau mỗi message, cập nhật chain key.
- Lưu metadata cần thiết để decrypt message cũ nhưng không lộ key tương lai.

### 5.6. Attachment E2EE

Chưa làm:

- Chưa mã hóa file/ảnh.
- Chưa upload attachment.

Cần làm:

- Sinh file key hoặc dùng message key riêng cho attachment.
- Encrypt file ở client.
- Upload ciphertext lên server.
- Lưu metadata như `iv`, `aad`, `mimeType`, `size`.

### 5.7. Pagination và đồng bộ message

Chưa làm:

- Chưa phân trang message rõ ràng.
- Chưa xử lý load thêm message cũ.

Cần làm:

- Thêm query `page`, `size`, hoặc cursor.
- UI load thêm khi kéo lên.
- Không decrypt lại toàn bộ nếu không cần.

### 5.8. UI/UX chat hoàn thiện

Chưa làm:

- UI mới ở mức chạy chức năng.
- Chưa có trạng thái gửi, đã gửi, lỗi gửi.
- Chưa có timestamp đẹp.
- Chưa có preview message ở conversation list.

Cần làm:

- Thêm trạng thái message pending/sent/failed.
- Thêm timestamp.
- Thêm preview message sau khi decrypt.
- Tối ưu layout cho màn hình nhỏ và máy thật.

### 5.9. Test

Chưa làm:

- Chưa có unit test cho crypto.
- Chưa có test repository.
- Chưa có test UI flow.

Cần làm:

- Test AES-GCM encrypt/decrypt.
- Test RSA wrap/unwrap conversation key.
- Test tạo conversation request đúng format.
- Test decrypt message list.
- Test login -> PIN -> conversation list -> chat detail.

## 6. Trạng thái hiện tại

Đã chạy được các phần chính:

- Login/PIN từ phần Auth.
- Vào danh sách conversation.
- Tạo conversation.
- Mở chat detail.
- Encrypt/decrypt message bằng `conversationKey`.
- Gọi backend online mới.

Các điểm cần lưu ý:

- Backend hiện tại phải trả đúng `encryptedConversationKey` và message ciphertext.
- Nếu backend trả lỗi 401 sai nguyên nhân, FE sẽ hiển thị lỗi auth dù token đang có.
- Trước đó khi test local, lỗi lớn từng nằm ở backend lazy loading, không phải do FE thiếu token.
- WebSocket mới là nền, chưa phải phần ổn định nhất.

## 7. Kết luận

Phần Frontend Chat E2EE hiện tại đã hoàn thành mức MVP cho luồng REST:

```text
Đăng nhập
-> Nhập PIN
-> Lấy private key đã unlock
-> Vào danh sách chat
-> Tạo conversation
-> Sinh và mã hóa conversationKey
-> Mở chat
-> Giải mã conversationKey
-> Đọc/gửi message đã mã hóa
```

Phần cần làm tiếp là hoàn thiện realtime WebSocket, multi-session, key recovery, key rotation, attachment E2EE, pagination và test.
