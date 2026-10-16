package com.vn.datphong.dto.response;

import com.vn.datphong.enums.RoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomDetailQuoteResponse {
    private Long roomId;
    private String code;
    private String name;
    private RoomType type;
    private Integer capacity;
    private String description;

    // Media & Assets
    private String imageUrl;
    private List<String> images;
    private List<String> videos;

    // Giá và Báo giá (Quote)
    private BigDecimal basePricePerNight; // Giá tham khảo mặc định (nếu chưa chọn ngày)
    private BigDecimal subTotal;           // Tổng tiền phòng tính theo số đêm
    private BigDecimal taxAndFee;          // Phụ phí hoặc thuế (nếu có)
    private BigDecimal totalPrice;         // Tổng chi phí cuối cùng
    private String currency;               // Đơn vị tiền tệ (VD: "VND")
    private Integer numberOfNights;        // Số đêm lưu trú

    // Thời hạn và Chính sách
    private String quoteExpiresAt;         // Thời hạn hiệu lực của báo giá (VD: "Báo giá có hiệu lực trong 15 phút")
    private String cancellationPolicy;     // Tóm tắt chính sách hủy phòng theo thời điểm
}
