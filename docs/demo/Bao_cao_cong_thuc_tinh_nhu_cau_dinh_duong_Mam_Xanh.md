# BÁO CÁO KỸ THUẬT: CÔNG THỨC TÍNH NHU CẦU DINH DƯỠNG CÁ NHÂN

**Dự án:** Mâm Xanh — Vegetarian Support System  
**Thành phần:** Hồ sơ dinh dưỡng và tính toán nhu cầu dinh dưỡng hằng ngày  
**Tài liệu:** Mô tả nghiệp vụ, phương trình tính toán, quy tắc áp dụng và nguồn tham khảo  
**Ngày lập báo cáo:** 21/11/2026  
**Căn cứ:** Nội dung mô tả logic backend và các tài liệu tham chiếu được cung cấp. Báo cáo này không thay thế kết quả kiểm tra trực tiếp mã nguồn tại thời điểm xuất tài liệu.

---

## 1. Mục đích và phạm vi

Báo cáo trình bày đầy đủ cách hệ thống Mâm Xanh xác định **chín chỉ tiêu dinh dưỡng hằng ngày** trong hồ sơ người dùng: năng lượng, đạm, carbohydrate, chất béo, chất xơ, canxi, sắt, vitamin B12 và kẽm.

Công thức **không đơn thuần lấy chiều cao nhân với cân nặng**. Backend áp dụng phương trình **EER (Estimated Energy Requirement — nhu cầu năng lượng ước tính)** dựa trên tuổi, chiều cao, cân nặng, giới tính sinh học và mức vận động. Từ EER, ứng dụng tính khoảng khuyến nghị cho các chất đa lượng; một số chỉ tiêu khác được xác định theo tuổi và giới tính sinh học.

**Phạm vi hỗ trợ theo mô tả mã nguồn:** người dùng từ **18 đến 120 tuổi**, tính cả hai mốc. Người đúng 18 tuổi sử dụng một nhánh phương trình riêng; người từ 19 tuổi sử dụng bộ phương trình ở Mục 3.

## 2. Dữ liệu đầu vào và đơn vị

| Dữ liệu đầu vào | Ký hiệu | Đơn vị / giá trị | Vai trò |
|---|---|---|---|
| Tuổi | `A` | Năm | Tham số của EER và các mức khuyến nghị theo tuổi |
| Chiều cao | `H` | Centimét (cm) | Tham số của EER |
| Cân nặng | `W` | Kilôgam (kg) | Tham số của EER |
| Giới tính sinh học | — | Nữ / Nam | Lựa chọn bộ hệ số và một số mức tham chiếu |
| Mức vận động | — | Ít / Nhẹ / Vừa / Nhiều | Lựa chọn phương trình EER tương ứng |

**Đầu ra năng lượng:** `kcal/ngày` (kilocalorie mỗi ngày).  
**Đầu ra chất đa lượng và chất xơ:** `g/ngày` (gram mỗi ngày).  
**Đầu ra canxi, sắt, kẽm:** `mg/ngày` (miligram mỗi ngày).  
**Đầu ra vitamin B12:** `mcg/ngày` (microgram mỗi ngày, tương đương µg/ngày).

## 3. Các phương trình EER cho người từ 19 tuổi

Hệ thống chọn phương trình tương ứng với **giới tính sinh học** và **mức vận động**. Các hệ số dưới đây được ghi lại theo mô tả logic backend đã cung cấp. Dấu phẩy trong bảng là dấu thập phân; dấu chấm trong `1.004,82` là dấu phân tách hàng nghìn.

| Mức vận động | Nữ (`kcal/ngày`) | Nam (`kcal/ngày`) |
|---|---|---|
| **Ít vận động** | `584,90 − 7,01 × A + 5,72 × H + 11,71 × W` | `753,07 − 10,83 × A + 6,50 × H + 14,10 × W` |
| **Vận động nhẹ** | `575,77 − 7,01 × A + 6,60 × H + 12,14 × W` | `581,47 − 10,83 × A + 8,30 × H + 14,94 × W` |
| **Vận động vừa** | `710,25 − 7,01 × A + 6,54 × H + 12,34 × W` | `1.004,82 − 10,83 × A + 6,52 × H + 15,91 × W` |
| **Vận động nhiều** | `511,83 − 7,01 × A + 9,07 × H + 12,56 × W` | `−517,88 − 10,83 × A + 15,61 × H + 19,11 × W` |

Trong đó: `A` = tuổi (năm), `H` = chiều cao (cm), `W` = cân nặng (kg).

**Nguồn phương pháp:** bộ phương trình EER của National Academies được dẫn chiếu trong tài liệu/mã nguồn theo thông tin đã cung cấp; xem Mục 9.

### 3.1. Minh họa cách tính năng lượng

**Hồ sơ ví dụ:** nữ, 29 tuổi, cao 165 cm, nặng 60 kg, mức vận động vừa.

Áp dụng phương trình dành cho nữ vận động vừa:

```text
EER = 710,25 − 7,01 × tuổi + 6,54 × chiều cao + 12,34 × cân nặng
    = 710,25 − 7,01 × 29 + 6,54 × 165 + 12,34 × 60
    = 710,25 − 203,29 + 1.079,10 + 740,40
    = 2.326,46 kcal/ngày
```

**Kết quả tính toán:** `2.326,46 kcal/ngày`.  
**Kết quả hiển thị trên giao diện khi làm tròn đến số nguyên:** khoảng **2.326 kcal/ngày**.

### 3.2. Trường hợp người đúng 18 tuổi

Theo mô tả backend, người **đúng 18 tuổi** áp dụng **nhánh phương trình EER có bộ hệ số riêng** và **cộng thêm `20 kcal/ngày`** để tính chi phí tăng trưởng.

> **Giới hạn thông tin:** Nội dung đầu vào không cung cấp bảng hệ số cụ thể cho người 18 tuổi. Vì vậy báo cáo ghi nhận chính xác quy tắc phân nhánh và phần cộng thêm, **không tự suy diễn** phương trình chi tiết.

Người **dưới 18 tuổi** hoặc **trên 120 tuổi** nằm ngoài phạm vi hồ sơ dinh dưỡng được ứng dụng hỗ trợ theo thông tin đã cung cấp.

## 4. Quy tắc tính ba chất đa lượng (macronutrients)

**Macronutrients (chất dinh dưỡng đa lượng)** là các nhóm dưỡng chất cần với lượng tương đối lớn, gồm đạm (protein), carbohydrate và chất béo (fat). Ứng dụng trả về **khoảng tham chiếu** theo tỷ lệ năng lượng chứ không phải một mục tiêu gram cố định.

Tỷ lệ tham chiếu được mô tả dựa trên DRI (*Dietary Reference Intakes* — bộ mức tham chiếu dinh dưỡng) của National Academies.

| Chất đa lượng | Tỷ lệ năng lượng | Quy đổi năng lượng | Công thức khoảng `g/ngày` |
|---|---|---|---|
| **Đạm (protein)** | 10–35% | 4 kcal/g | `EER × 10% ÷ 4` đến `EER × 35% ÷ 4` |
| **Carbohydrate** | 45–65% | 4 kcal/g | `EER × 45% ÷ 4` đến `EER × 65% ÷ 4` |
| **Chất béo (fat)** | 20–35% | 9 kcal/g | `EER × 20% ÷ 9` đến `EER × 35% ÷ 9` |

Các hệ số **4 kcal/g** và **9 kcal/g** dùng để quy đổi năng lượng thành khối lượng chất dinh dưỡng.

### 4.1. Kết quả trên hồ sơ ví dụ

Với `EER = 2.326,46 kcal/ngày`:

**Đạm (protein):**

```text
Cận dưới = 2.326,46 × 10% ÷ 4 = 58,1615 ≈ 58,16 g/ngày
Cận trên = 2.326,46 × 35% ÷ 4 = 203,56525 ≈ 203,57 g/ngày
```

**Carbohydrate:**

```text
Cận dưới = 2.326,46 × 45% ÷ 4 = 261,72675 ≈ 261,73 g/ngày
Cận trên = 2.326,46 × 65% ÷ 4 = 378,04975 ≈ 378,05 g/ngày
```

**Chất béo:**

```text
Cận dưới = 2.326,46 × 20% ÷ 9 = 51,69911… ≈ 51,70 g/ngày
Cận trên = 2.326,46 × 35% ÷ 9 = 90,47344… ≈ 90,47 g/ngày
```

**Tổng hợp khoảng tham chiếu của ví dụ:**

| Chỉ tiêu | Khoảng hằng ngày |
|---|---:|
| Đạm | **58,16–203,57 g** |
| Carbohydrate | **261,73–378,05 g** |
| Chất béo | **51,70–90,47 g** |

**Lưu ý về ý nghĩa:** đây là ba khoảng tham chiếu riêng. Không nên hiểu rằng phải đồng thời chọn cận trên của cả ba nhóm vì tổng tỷ lệ năng lượng có thể vượt 100%.

## 5. Quy tắc tính chất xơ và các vi chất

Theo logic backend được mô tả, các mục dưới đây dựa trên **tuổi và giới tính sinh học**; **mức vận động không làm thay đổi** giá trị của chúng.

| Chỉ tiêu | Quy tắc theo hồ sơ | Giá trị ở ví dụ nữ 29 tuổi |
|---|---|---:|
| **Chất xơ** | Nữ 19–50 tuổi: **25 g**; nam 19–50 tuổi: **38 g**. Từ 51 tuổi: nữ **21 g**, nam **30 g**. | **25 g/ngày** |
| **Canxi** | Người 18 tuổi: **1.300 mg**. Nữ từ 51 tuổi hoặc người từ 71 tuổi: **1.200 mg**. Các trường hợp còn lại trong phạm vi: **1.000 mg**. | **1.000 mg/ngày** |
| **Sắt** | Nữ 19–50 tuổi: **18 mg**. Nam từ 19 tuổi và người từ 51 tuổi: **8 mg**. Người 18 tuổi: nam **11 mg**, nữ **15 mg**. | **18 mg/ngày** |
| **Vitamin B12** | Cố định **2,4 mcg** cho tất cả hồ sơ trong phạm vi được hỗ trợ. | **2,4 mcg/ngày** |
| **Kẽm** | Từ 19 tuổi: nam **11 mg**, nữ **8 mg**. Người 18 tuổi: nam **11 mg**, nữ **9 mg**. | **8 mg/ngày** |

**Chú thích về phạm vi:** Mô tả đầu vào không nêu riêng quy tắc chất xơ cho người đúng 18 tuổi; báo cáo không bổ sung một giá trị chưa được xác nhận từ mã nguồn.

Các mức B12 và sắt ở người trưởng thành được đối chiếu trong nội dung gốc với tài liệu NIH ODS (*Office of Dietary Supplements* — Văn phòng Thực phẩm bổ sung của Viện Y tế Quốc gia Hoa Kỳ); ví dụ B12 là `2,4 mcg/ngày` cho người trưởng thành và sắt là `18 mg/ngày` cho nữ 19–50 tuổi.

## 6. Danh mục chín chỉ tiêu trên giao diện

| STT | Chỉ tiêu | Đơn vị | Cách xác định | Kết quả ví dụ |
|---:|---|---|---|---:|
| 1 | **Năng lượng** | kcal/ngày | EER theo tuổi, chiều cao, cân nặng, giới tính sinh học, mức vận động | **2.326,46** (hiển thị khoảng **2.326**) |
| 2 | **Đạm** | g/ngày | 10–35% EER, chia 4 | **58,16–203,57** |
| 3 | **Carbohydrate** | g/ngày | 45–65% EER, chia 4 | **261,73–378,05** |
| 4 | **Chất béo** | g/ngày | 20–35% EER, chia 9 | **51,70–90,47** |
| 5 | **Chất xơ** | g/ngày | Theo tuổi và giới tính sinh học | **25** |
| 6 | **Canxi** | mg/ngày | Theo tuổi và giới tính sinh học | **1.000** |
| 7 | **Sắt** | mg/ngày | Theo tuổi và giới tính sinh học | **18** |
| 8 | **Vitamin B12** | mcg/ngày | Hằng số trong phạm vi hỗ trợ | **2,4** |
| 9 | **Kẽm** | mg/ngày | Theo tuổi và giới tính sinh học | **8** |

Trong **năm mục cuối**, **chất xơ** được tính riêng, còn **bốn vi chất** được liệt kê là **canxi, sắt, vitamin B12 và kẽm**.

## 7. Phân biệt giá trị tham chiếu với lượng dinh dưỡng tiêu thụ

Các chỉ tiêu trong hồ sơ là **mục tiêu hoặc mức tham chiếu dinh dưỡng hằng ngày**, **không phải số liệu đo lường thực tế người dùng đã ăn**.

- **Hồ sơ dinh dưỡng**: dùng thông tin cá nhân để ước tính mức năng lượng và dưỡng chất tham chiếu, hỗ trợ cá nhân hóa và lập thực đơn.
- **Dinh dưỡng món ăn**: được hệ thống tính riêng dựa trên dữ liệu nguyên liệu cấu thành món ăn.

Do đó, không được đồng nhất `nhu cầu khuyến nghị` với `lượng tiêu thụ thực tế` khi mô tả nghiệp vụ, thiết kế giao diện hoặc giải thích kết quả cho người dùng.

## 8. Những lưu ý khi sử dụng và diễn giải kết quả

1. **EER là ước tính, không phải phép đo chính xác tuyệt đối**. Kết quả có thể khác nhu cầu thực tế của từng cá nhân.
2. **Giới tính sinh học và mức vận động ảnh hưởng đến EER** theo bộ hệ số; tuổi, chiều cao, cân nặng cũng đi trực tiếp vào phương trình.
3. **Khoảng protein, carbohydrate và chất béo phụ thuộc vào EER**; khi EER thay đổi, các khoảng tính theo gram cũng thay đổi.
4. **Chất xơ, canxi, sắt, B12, kẽm** được tính theo các quy tắc tuổi/giới tính hoặc hằng số được mô tả, không phụ thuộc mức vận động trong logic hiện tại.
5. **Làm tròn trên giao diện** có thể khiến số hiển thị khác kết quả trung gian; nên giữ đủ độ chính xác trong bước tính trước khi định dạng để hiển thị.
6. **Người đúng 18 tuổi** có nhánh EER riêng và cộng thêm `20 kcal/ngày`; không áp dụng nguyên xi bảng người từ 19 tuổi.
7. Các giá trị là **tham khảo**, **không thay thế việc đánh giá y khoa hoặc tư vấn dinh dưỡng cá nhân**, nhất là đối với trường hợp có nhu cầu đặc biệt.

## 9. Nguồn tham khảo và vị trí mã nguồn

### 9.1. Tài liệu tham khảo

1. **National Academies — Dietary Reference Intakes for Energy**. Phần phương trình EER trong tài liệu:  
   https://www.nationalacademies.org/read/26818/chapter/7
2. **National Academies Press — Dietary Reference Intakes for Energy, Carbohydrate, Fiber, Fat, Fatty Acids, Cholesterol, Protein, and Amino Acids**. Nguồn tham khảo về tỷ lệ chất đa lượng:  
   https://nap.nationalacademies.org/catalog/10490/
3. **NIH ODS — Vitamin B12: Health Professional Fact Sheet**:  
   https://ods.od.nih.gov/factsheets/VitaminB12-HealthProfessional/
4. **NIH ODS — Iron: Consumer Fact Sheet**:  
   https://ods.od.nih.gov/factsheets/Iron-Consumer/

### 9.2. Đường dẫn mã nguồn được cung cấp

**Tệp:** `NutritionCalculationService.java` — tham chiếu từ **dòng 38** trong thông tin gốc.

```text
D:\Semester 5\SWP391\vegetarian-support-system\app\mamxanh-backend\src\main\java\tech\mamxanh\nutrition\service\NutritionCalculationService.java
```

> Các đường dẫn và nguồn trên được giữ lại theo thông tin đầu vào. Báo cáo này không khẳng định đã mở, kiểm thử hoặc đối chiếu trực tiếp phiên bản mã nguồn hiện hành; khi nghiệm thu nên kiểm tra lại các công thức với tệp thực tế.

## 10. Kết luận

Hồ sơ dinh dưỡng Mâm Xanh sử dụng **EER theo các tham số nhân trắc và vận động** để xác định nhu cầu năng lượng hằng ngày. Năng lượng này là cơ sở tính **khoảng khuyến nghị cho ba chất đa lượng**. Các chỉ tiêu **chất xơ, canxi, sắt, vitamin B12 và kẽm** được lấy từ các quy tắc tuổi/giới tính sinh học hoặc một mức cố định theo logic ứng dụng được mô tả.

Với hồ sơ mẫu **nữ, 29 tuổi, 165 cm, 60 kg, vận động vừa**, EER tính được là **2.326,46 kcal/ngày**; giao diện làm tròn khoảng **2.326 kcal/ngày**. Báo cáo giữ nguyên toàn bộ các hệ số EER người từ 19 tuổi, nhánh đặc biệt tuổi 18, khoảng ba chất đa lượng, năm chỉ tiêu còn lại, định nghĩa chín mục hiển thị, phạm vi hỗ trợ và các nguồn tham khảo trong nội dung được cung cấp.
