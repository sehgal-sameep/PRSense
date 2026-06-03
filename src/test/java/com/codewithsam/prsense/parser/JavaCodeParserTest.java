package com.codewithsam.prsense.parser;

import com.codewithsam.prsense.constants.SymbolType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JavaCodeParserTest {

    private JavaCodeParser parser;

    @BeforeEach
    void setUp() {
        parser = new JavaCodeParser();
    }

    @Test
    void supports_javaFiles() {
        assertThat(parser.supports("src/main/UserService.java")).isTrue();
        assertThat(parser.supports("src/main/UserService.kt")).isFalse();
        assertThat(parser.supports(null)).isFalse();
    }

    @Test
    void parse_extractsClassAndMethods() {
        String source = """
                package com.example;

                public class UserService {
                    private final UserRepository userRepository;

                    public UserService(UserRepository userRepository) {
                        this.userRepository = userRepository;
                    }

                    public User createUser(String name) {
                        return userRepository.save(new User(name));
                    }

                    public void deleteUser(Long id) {
                        userRepository.deleteById(id);
                    }
                }
                """;

        List<ParsedSymbol> symbols = parser.parse("UserService.java", source);

        assertThat(symbols).isNotEmpty();

        Optional<ParsedSymbol> classSymbol = symbols.stream()
                .filter(s -> s.getSymbolType() == SymbolType.CLASS)
                .findFirst();
        assertThat(classSymbol).isPresent();
        assertThat(classSymbol.get().getClassName()).isEqualTo("UserService");
        assertThat(classSymbol.get().getPackageName()).isEqualTo("com.example");

        long methodCount = symbols.stream()
                .filter(s -> s.getSymbolType() == SymbolType.METHOD)
                .count();
        assertThat(methodCount).isEqualTo(2);

        long constructorCount = symbols.stream()
                .filter(s -> s.getSymbolType() == SymbolType.CONSTRUCTOR)
                .count();
        assertThat(constructorCount).isEqualTo(1);
    }

    @Test
    void parse_extractsInterface() {
        String source = """
                package com.example;

                public interface UserRepository {
                    User findById(Long id);
                    void deleteById(Long id);
                }
                """;

        List<ParsedSymbol> symbols = parser.parse("UserRepository.java", source);

        Optional<ParsedSymbol> interfaceSymbol = symbols.stream()
                .filter(s -> s.getSymbolType() == SymbolType.INTERFACE)
                .findFirst();
        assertThat(interfaceSymbol).isPresent();
        assertThat(interfaceSymbol.get().getClassName()).isEqualTo("UserRepository");
    }

    @Test
    void parse_extractsEnum() {
        String source = """
                package com.example;

                public enum Status {
                    ACTIVE, INACTIVE, DELETED
                }
                """;

        List<ParsedSymbol> symbols = parser.parse("Status.java", source);

        Optional<ParsedSymbol> enumSymbol = symbols.stream()
                .filter(s -> s.getSymbolType() == SymbolType.ENUM)
                .findFirst();
        assertThat(enumSymbol).isPresent();
        assertThat(enumSymbol.get().getClassName()).isEqualTo("Status");
    }

    @Test
    void parse_extractsRecord() {
        String source = """
                package com.example;

                public record UserDto(Long id, String name, String email) {}
                """;

        List<ParsedSymbol> symbols = parser.parse("UserDto.java", source);

        Optional<ParsedSymbol> recordSymbol = symbols.stream()
                .filter(s -> s.getSymbolType() == SymbolType.RECORD)
                .findFirst();
        assertThat(recordSymbol).isPresent();
        assertThat(recordSymbol.get().getClassName()).isEqualTo("UserDto");
    }

    @Test
    void parse_returnsEmptyListForUnparsableSource() {
        List<ParsedSymbol> symbols = parser.parse("Bad.java", "this is not valid java }{{{");
        assertThat(symbols).isEmpty();
    }

    @Test
    void parse_methodContentIncludesClassContext() {
        String source = """
                package com.example;

                public class OrderService {
                    public void placeOrder(Long userId) {
                        // place order logic
                    }
                }
                """;

        List<ParsedSymbol> symbols = parser.parse("OrderService.java", source);

        Optional<ParsedSymbol> methodSymbol = symbols.stream()
                .filter(s -> s.getSymbolType() == SymbolType.METHOD && "placeOrder".equals(s.getMethodName()))
                .findFirst();

        assertThat(methodSymbol).isPresent();
        assertThat(methodSymbol.get().getContent()).contains("OrderService");
        assertThat(methodSymbol.get().getContent()).contains("placeOrder");
    }
}
