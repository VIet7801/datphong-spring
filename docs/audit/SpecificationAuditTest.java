// Probe đối chiếu hành vi theo đặc tả v3.0, không phải code sản phẩm.
// Giữ ngoài src/test vì các assertion này chủ động phơi bày thiếu sót chưa xử lý.
package com.vn.datphong.audit;

import com.vn.datphong.config.SecurityConfig;
import com.vn.datphong.controller.RoomController;
import com.vn.datphong.dto.request.RoomCreateRequest;
import com.vn.datphong.dto.request.RoomUpdateRequest;
import com.vn.datphong.dto.request.UsersResquest;
import com.vn.datphong.dto.response.RoomResponse;
import com.vn.datphong.entity.Room;
import com.vn.datphong.entity.RoomRate;
import com.vn.datphong.entity.RoomTypeEntity;
import com.vn.datphong.entity.Property;
import com.vn.datphong.entity.User;
import com.vn.datphong.enums.RoomStatus;
import com.vn.datphong.enums.PropertyStatus;
import com.vn.datphong.exception.BusinessException;
import com.vn.datphong.exception.GlobalExceptionHandler;
import com.vn.datphong.repository.RoomImageRepository;
import com.vn.datphong.repository.RoomRepository;
import com.vn.datphong.repository.RoomRateRepository;
import com.vn.datphong.repository.UserRepository;
import com.vn.datphong.security.JwtTokenProvider;
import com.vn.datphong.service.Impl.CustomUserDetailsService;
import com.vn.datphong.service.Impl.RoomServiceImp;
import com.vn.datphong.service.Impl.UserServiceImpl;
import com.vn.datphong.service.MinioService;
import com.vn.datphong.service.RoomService;
import jakarta.servlet.http.Part;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringJUnitConfig(SpecificationAuditTest.Config.class)
@WebAppConfiguration
class SpecificationAuditTest {
    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, RoomController.class, CustomUserDetailsService.class,
            GlobalExceptionHandler.class})
    static class Config {
        @Bean UserRepository userRepository() { return mock(UserRepository.class); }
        @Bean RoomService roomService() { return mock(RoomService.class); }
        @Bean JwtTokenProvider jwtTokenProvider() { return mock(JwtTokenProvider.class); }
    }

    @Autowired private WebApplicationContext context;
    @Autowired private RoomService roomService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(roomService);
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void uc07AnonymousGuestCanSearchRooms() throws Exception {
        mvc.perform(get("/api/rooms")
                        .param("checkIn", LocalDate.now().plusDays(10).toString())
                        .param("checkOut", LocalDate.now().plusDays(11).toString()))
                .andExpect(status().isOk());
    }

    @Test
    void uc05CustomerCannotCreateRooms() throws Exception {
        when(roomService.create(any())).thenReturn(RoomResponse.builder().id(1L).build());
        mvc.perform(post("/api/rooms/create/rooms").with(user("customer").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"AUDIT-1","name":"Audit room","type":"SINGLE",
                                 "capacity":2,"pricePerNight":100000}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void uc05RoomUpdateChangesTheStoredEntity() {
        RoomRepository rooms = mock(RoomRepository.class);
        RoomImageRepository images = mock(RoomImageRepository.class);
        Room room = Room.builder().id(1L).name("Original name").build();
        when(rooms.findById(1L)).thenReturn(Optional.of(room));
        when(rooms.save(any())).thenAnswer(call -> call.getArgument(0));
        when(images.findByRoomIdOrdered(1L)).thenReturn(List.of());
        RoomServiceImp service = new RoomServiceImp(rooms, mock(MinioService.class), images, mock(RoomRateRepository.class));

        RoomResponse result = service.update(1L, RoomUpdateRequest.builder().name("Updated name").build());

        assertEquals("Updated name", result.getName());
        assertEquals("Updated name", room.getName());
    }

    @Test
    void uc01UserCreationHashesPasswordBeforeSave() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
        UserServiceImpl service = new UserServiceImpl();
        ReflectionTestUtils.setField(service, "userRepository", repository);

        service.create(UsersResquest.builder().username("audit-user").password("audit-password").build());

        ArgumentCaptor<User> captured = ArgumentCaptor.forClass(User.class);
        verify(repository).save(captured.capture());
        assertNotEquals("audit-password", captured.getValue().getPassword(),
                "Password must not be saved as plain text");
        assertTrue(new BCryptPasswordEncoder().matches("audit-password", captured.getValue().getPassword()));
    }

    @Test
    void uc06UploadPersistsMediaMetadata() throws Exception {
        RoomRepository rooms = mock(RoomRepository.class);
        RoomImageRepository images = mock(RoomImageRepository.class);
        MinioService minio = mock(MinioService.class);
        when(rooms.findById(1L)).thenReturn(Optional.of(Room.builder().id(1L).build()));
        when(minio.getBucket()).thenReturn("audit-bucket");
        when(minio.getObjectUrl(anyString(), anyString())).thenReturn("http://example.test/image.png");
        Part part = mock(Part.class);
        byte[] png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a7X8AAAAASUVORK5CYII=");
        when(part.getSubmittedFileName()).thenReturn("image.png");
        when(part.getContentType()).thenReturn("image/png");
        when(part.getSize()).thenReturn((long) png.length);
        when(part.getInputStream()).thenReturn(new ByteArrayInputStream(png));
        RoomServiceImp service = new RoomServiceImp(rooms, minio, images, mock(RoomRateRepository.class));

        service.uploadMedia(1L, part);

        verify(images).save(any());
    }

    @Test
    void uc06UploadEndpointAcceptsMultipartFile() throws Exception {
        when(roomService.uploadMedia(eq(1L), any())).thenReturn("http://example.test/image.png");
        // Controller nhận Servlet Part: mô phỏng getPart() của container, không chỉ MultipartFile.
        MockPart part = new MockPart("file", "image.png", new byte[]{1});
        part.getHeaders().setContentType(MediaType.IMAGE_PNG);
        mvc.perform(multipart("/api/rooms/media/1")
                        .part(part)
                        .with(user("host").roles("HOST")))
                .andExpect(status().isCreated());
        ArgumentCaptor<Part> captured = ArgumentCaptor.forClass(Part.class);
        verify(roomService).uploadMedia(eq(1L), captured.capture());
        assertNotNull(captured.getValue(), "Multipart file must bind to a non-null Part");
        assertEquals("image.png", captured.getValue().getSubmittedFileName());
    }

    @Test
    void uc05CreatedRoomHasRequiredPropertyAndRoomTypeRelations() {
        RoomRepository rooms = mock(RoomRepository.class);
        when(rooms.save(any())).thenAnswer(call -> call.getArgument(0));
        RoomServiceImp service = new RoomServiceImp(rooms, mock(MinioService.class),
                mock(RoomImageRepository.class), mock(RoomRateRepository.class));
        service.create(RoomCreateRequest.builder().propertyId(10L).roomTypeId(20L).code("AUDIT-FK").build());
        ArgumentCaptor<Room> captured = ArgumentCaptor.forClass(Room.class);
        verify(rooms).save(captured.capture());
        assertAll(
                () -> assertNotNull(captured.getValue().getProperty(), "Phòng phải được gắn property hợp lệ"),
                () -> assertNotNull(captured.getValue().getRoomTypeEntity(), "Phòng phải được gắn loại phòng hợp lệ"));
    }

    @Test
    void uc08DraftPropertyCannotBeShownAsPublicRoomDetail() {
        Room room = quoteRoom();
        room.getProperty().setStatus(PropertyStatus.DRAFT);
        RoomServiceImp service = quoteService(room, mock(RoomRateRepository.class));
        assertThrows(BusinessException.class, () -> service.getDetailAndQuote(1L, null, null, 1));
    }

    @Test
    void uc08QuoteRejectsZeroGuests() {
        RoomRateRepository rates = mock(RoomRateRepository.class);
        LocalDate from = LocalDate.now().plusDays(10);
        when(rates.findRatesByRoomAndDateRange(20L, from, from.plusDays(1)))
                .thenReturn(List.of(RoomRate.builder().stayDate(from).nightlyRate(new BigDecimal("100000")).build()));
        RoomServiceImp service = quoteService(quoteRoom(), rates);
        assertThrows(BusinessException.class, () -> service.getDetailAndQuote(1L, from, from.plusDays(1), 0));
    }

    @Test
    void uc08QuoteRejectsAnIncompleteDatePair() {
        RoomServiceImp service = quoteService(quoteRoom(), mock(RoomRateRepository.class));
        assertThrows(BusinessException.class,
                () -> service.getDetailAndQuote(1L, LocalDate.now().plusDays(10), null, 1));
    }

    @Test
    void uc08QuoteSumsEachNightAndUsesRoomTypeWithHalfOpenDateRange() {
        RoomRateRepository rates = mock(RoomRateRepository.class);
        LocalDate from = LocalDate.now().plusDays(10);
        when(rates.findRatesByRoomAndDateRange(20L, from, from.plusDays(3))).thenReturn(List.of(
                RoomRate.builder().stayDate(from).nightlyRate(new BigDecimal("100000")).build(),
                RoomRate.builder().stayDate(from.plusDays(1)).nightlyRate(new BigDecimal("150000")).build(),
                RoomRate.builder().stayDate(from.plusDays(2)).nightlyRate(new BigDecimal("200000")).build()));
        var quote = quoteService(quoteRoom(), rates).getDetailAndQuote(1L, from, from.plusDays(3), 2);
        assertEquals(0, new BigDecimal("450000").compareTo(quote.getSubTotal()));
        assertEquals(3, quote.getNumberOfNights());
        verify(rates).findRatesByRoomAndDateRange(20L, from, from.plusDays(3));
    }

    private Room quoteRoom() {
        return Room.builder().id(1L).active(true).status(RoomStatus.ACTIVE).capacity(2)
                .property(Property.builder().id(10L).status(PropertyStatus.PUBLISHED).build())
                .roomTypeEntity(RoomTypeEntity.builder().id(20L).maxGuests(2).active(true).build()).build();
    }

    private RoomServiceImp quoteService(Room room, RoomRateRepository rates) {
        RoomRepository rooms = mock(RoomRepository.class);
        when(rooms.findById(1L)).thenReturn(Optional.of(room));
        RoomImageRepository images = mock(RoomImageRepository.class);
        when(images.findByRoomIdOrdered(1L)).thenReturn(List.of());
        return new RoomServiceImp(rooms, mock(MinioService.class), images, rates);
    }
}
