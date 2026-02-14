# Requirements Document: Maven MCP Server

**Version:** 1.0
**Date:** 2026-02-14
**Status:** Approved
**Stakeholder(s):** Mariusz (Developer)

---

## 1. Executive Summary

Serwer MCP (Model Context Protocol) umożliwiający Claude Code autonomiczne wykonywanie operacji Maven — kompilację, uruchamianie testów, budowanie i czyszczenie projektu. Serwer parsuje output Mavena i zwraca ustrukturyzowane informacje o błędach, dzięki czemu AI agent może szybciej identyfikować i naprawiać problemy.

## 2. Problem Statement

Obecnie Claude Code nie ma bezpośredniego, ustrukturyzowanego dostępu do operacji Maven. Agent musi polegać na surowym output z terminala, co:
- Wymaga ręcznej interpretacji długich logów Mavena
- Utrudnia szybkie wyłapywanie błędów kompilacji i testów
- Spowalnia cykl: edycja → kompilacja → naprawa → ponowna kompilacja
- Wymaga od agenta "zgadywania" jakie komendy uruchomić

Sparsowany output pozwoli agentowi natychmiast zlokalizować błąd (plik, linia, komunikat) i podjąć akcję naprawczą.

## 3. Goals & Success Criteria

| Goal | Measurable Success Criterion |
|------|------------------------------|
| Szybsze wykrywanie błędów kompilacji | Agent otrzymuje listę błędów ze ścieżką pliku i numerem linii — bez ręcznego parsowania logów |
| Sprawne uruchamianie testów | Agent może odpalić konkretny test lub zestaw testów i otrzymać wynik (pass/fail + komunikat) |
| Autonomiczny cykl build | Agent może samodzielnie wykonać: clean → compile → test → package bez interwencji użytkownika |
| Prostota konfiguracji | Serwer da się uruchomić jedną linią konfiguracji w Claude Code |

## 4. Scope

### 4.1 In Scope
- Operacje Maven: `compile`, `test`, `package`, `clean`
- Uruchamianie konkretnych testów (klasa, metoda, wiele testów)
- Parsowanie output Maven: błędy kompilacji, wyniki testów
- Obsługa Maven Wrapper (`./mvnw`) z fallbackiem na systemowy `mvn`
- Przekazywanie dowolnych dodatkowych flag Maven
- Konfigurowalny timeout operacji
- Transport: stdio (standard Claude Code)
- Konfiguracja ścieżki projektu przez argument CLI

### 4.2 Out of Scope
- Obsługa projektów multi-module (operacje na konkretnym module via `-pl`)
- Zarządzanie pom.xml (edycja zależności, wersji)
- Wsparcie dla Gradle lub innych build toolów
- GUI / interfejs webowy
- Uruchamianie serwera dla wielu projektów jednocześnie

## 5. Users & Personas

| Persona / Role | Description | Key Goals |
|----------------|-------------|-----------|
| Claude Code Agent | AI agent działający w terminalu, główny konsument API serwera | Kompilować kod, uruchamiać testy, interpretować błędy, budować artefakty |
| Developer (konfiguracja) | Programista konfigurujący serwer w swoim środowisku | Szybka konfiguracja, minimalna ilość parametrów |

## 6. Functional Requirements

### FR-001: Kompilacja projektu
- **Description:** Serwer udostępnia tool do kompilacji projektu Maven
- **User Story:** As a Claude Code Agent, I need to compile the project so that I can detect compilation errors after code changes.
- **Acceptance Criteria:**
  - [ ] Tool `maven_compile` wywołuje `mvn compile` (lub `./mvnw compile`)
  - [ ] Zwraca status: SUCCESS / FAILURE
  - [ ] W przypadku błędów zwraca sparsowaną listę: `[{file, line, column?, message, severity}]`
  - [ ] Obsługuje dodatkowe flagi Maven (np. `-DskipFrontend`)
- **Priority:** Must
- **Notes:** Ścieżki plików w błędach powinny być względne do katalogu projektu

### FR-002: Uruchamianie testów
- **Description:** Serwer udostępnia tool do uruchamiania testów Maven
- **User Story:** As a Claude Code Agent, I need to run specific tests so that I can verify fixes and detect regressions.
- **Acceptance Criteria:**
  - [ ] Tool `maven_test` wywołuje `mvn test`
  - [ ] Parametr `testFilter` pozwala na uruchomienie: konkretnej klasy (`MyTest`), konkretnej metody (`MyTest#method`), wielu testów (`MyTest,OtherTest`)
  - [ ] Zwraca status: SUCCESS / FAILURE
  - [ ] Zwraca sparsowany wynik: `{testsRun, testsFailed, testsSkipped, testsErrored, failures: [{testClass, testMethod, message, stackTrace?}]}`
  - [ ] W przypadku błędów kompilacji (testy się nie skompilowały) — zwraca błędy kompilacji jak w FR-001
  - [ ] Obsługuje dodatkowe flagi Maven
- **Priority:** Must

### FR-003: Budowanie pakietu
- **Description:** Serwer udostępnia tool do budowania pełnego pakietu (JAR/WAR)
- **User Story:** As a Claude Code Agent, I need to build the project package so that I can verify the full build succeeds.
- **Acceptance Criteria:**
  - [ ] Tool `maven_package` wywołuje `mvn package`
  - [ ] Zwraca status: SUCCESS / FAILURE
  - [ ] W przypadku błędów — sparsowane błędy kompilacji lub testów
  - [ ] Zwraca ścieżkę do zbudowanego artefaktu (jeśli sukces)
  - [ ] Obsługuje dodatkowe flagi Maven (np. `-DskipTests`)
- **Priority:** Must

### FR-004: Czyszczenie projektu
- **Description:** Serwer udostępnia tool do czyszczenia katalogu build
- **User Story:** As a Claude Code Agent, I need to clean the build directory so that I can ensure a fresh build.
- **Acceptance Criteria:**
  - [ ] Tool `maven_clean` wywołuje `mvn clean`
  - [ ] Zwraca status: SUCCESS / FAILURE
  - [ ] Obsługuje dodatkowe flagi Maven
- **Priority:** Must

### FR-005: Wykrywanie Maven Wrapper
- **Description:** Serwer automatycznie wykrywa obecność Maven Wrapper i używa go priorytetowo
- **Acceptance Criteria:**
  - [ ] Jeśli w katalogu projektu istnieje `./mvnw` (Linux/Mac) lub `mvnw.cmd` (Windows) — używa wrappera
  - [ ] Jeśli wrapper nie istnieje — fallback na systemowy `mvn`
  - [ ] Jeśli ani wrapper, ani `mvn` nie są dostępne — zwraca czytelny komunikat błędu
- **Priority:** Must

### FR-006: Timeout operacji
- **Description:** Każda operacja Maven ma konfigurowalny timeout
- **Acceptance Criteria:**
  - [ ] Domyślny timeout (np. 300s) konfigurowalny przy starcie serwera
  - [ ] Możliwość nadpisania timeout per wywołanie (parametr toola)
  - [ ] Po przekroczeniu timeout — proces Maven jest zabijany, zwracany jest status TIMEOUT z dotychczasowym outputem
- **Priority:** Should

### FR-007: Przekazywanie dodatkowych flag Maven
- **Description:** Każdy tool akceptuje opcjonalny parametr z dodatkowymi flagami Maven
- **Acceptance Criteria:**
  - [ ] Parametr `args` (lista stringów) dołączany do komendy Maven
  - [ ] Przykłady: `["-DskipTests"]`, `["-X"]`, `["-DskipFrontend", "-Pdev"]`
  - [ ] Niebezpieczne flagi (np. deploy) — brak ograniczeń, zaufanie do agenta
- **Priority:** Must

## 7. Data Requirements

### 7.1 Key Entities

| Entity | Description | Key Attributes |
|--------|-------------|----------------|
| CompilationError | Błąd kompilacji Java | file, line, column, message, severity (ERROR/WARNING) |
| TestResult | Wynik uruchomienia testów | testsRun, testsFailed, testsSkipped, testsErrored |
| TestFailure | Pojedynczy failed/errored test | testClass, testMethod, message, stackTrace |
| BuildResult | Ogólny wynik operacji | status (SUCCESS/FAILURE/TIMEOUT), duration, errors[], output? |
| ArtifactInfo | Info o zbudowanym artefakcie | path, name, size |

### 7.2 Data Sources & Integrations

| Source / System | Direction | Data Exchanged | Protocol / Format |
|-----------------|-----------|----------------|-------------------|
| Maven CLI / mvnw | Outbound → Inbound | Komendy Maven → stdout/stderr | Process spawn (child_process) |
| Claude Code | Inbound → Outbound | Tool calls → Sparsowane wyniki | MCP over stdio |

### 7.3 Data Retention & Lifecycle
- Brak persystencji — serwer jest stateless
- Output Maven jest przetwarzany w locie i zwracany jako odpowiedź toola

## 8. Non-Functional Requirements

### 8.1 Performance

| Metric | Target |
|--------|--------|
| Narzut serwera (overhead ponad sam Maven) | < 500ms |
| Czas parsowania output | < 1s dla typowego builda |

### 8.2 Availability & Reliability
- Serwer działa jako proces potomny Claude Code — dostępność = dostępność sesji Claude Code
- Graceful handling: jeśli Maven się zawiesi, timeout zabija proces i zwraca częściowy output

### 8.3 Security & Compliance
- Serwer wykonuje komendy Maven lokalnie — ten sam kontekst bezpieczeństwa co użytkownik
- Brak uwierzytelniania (komunikacja przez stdio)
- **Walidacja ścieżki projektu:** upewnić się, że wskazuje na istniejący katalog z `pom.xml`

### 8.4 Scalability
- N/A — serwer obsługuje jeden projekt, jedną sesję Claude Code

### 8.5 Accessibility
- N/A — brak interfejsu użytkownika

### 8.6 Compatibility

| Platform / Environment | Minimum Version |
|------------------------|-----------------|
| Java (JDK) | 25+ |
| Maven | 3.9+ |
| OS | Linux, macOS (Windows nice-to-have) |

## 9. Constraints

### 9.1 Technical Constraints
- **Transport:** stdio (wymagane przez Claude Code)
- **Język:** Java 25+
- **Architektura:** Wrapper CLI — serwer uruchamia Maven jako zewnętrzny proces (`ProcessBuilder`), parsuje stdout/stderr
- **MCP SDK:** `io.modelcontextprotocol.sdk:mcp` (oficjalny Java SDK, v0.17+, wspiera stdio transport)
- **Build tool projektu MCP:** Maven (dogfooding — budujemy serwer Maven w Mavenie)
- **Brak multi-module:** serwer nie obsługuje Maven reactor / `-pl` operacji
- **Ewolucja:** Architektura wrapper CLI pozwala w przyszłości przejść na Maven Embedder (in-process) bez zmiany interfejsu MCP

### 9.2 Organizational Constraints
- Projekt open-source / wewnętrzny — jeden developer

### 9.3 Budget & Timeline
- **Budget:** N/A (projekt wewnętrzny)
- **Target Launch:** Możliwie szybko — prosty scope

### 9.4 Regulatory / Legal
- N/A

## 10. Assumptions & Dependencies

### Assumptions
1. Maven (lub Maven Wrapper) jest zainstalowany i skonfigurowany na maszynie użytkownika
2. Projekt Maven ma poprawny `pom.xml` w katalogu głównym
3. Claude Code obsługuje MCP serwery via stdio (standard od MCP 1.0)
4. Java 25+ jest dostępne na maszynie użytkownika
5. Projekty są single-module (brak reactor)

### Dependencies
- `io.modelcontextprotocol.sdk:mcp` — oficjalny Java SDK do budowy MCP serwerów (v0.17+)
- Jackson — serializacja JSON (wymagany przez MCP Java SDK)
- SLF4J — logging
- Maven CLI / mvnw — serwer jest wrapperem nad procesem Maven

## 11. Prioritization & Phasing

### MVP (Phase 1)
- FR-001: Kompilacja projektu (Must)
- FR-002: Uruchamianie testów (Must)
- FR-003: Budowanie pakietu (Must)
- FR-004: Czyszczenie projektu (Must)
- FR-005: Wykrywanie Maven Wrapper (Must)
- FR-007: Dodatkowe flagi Maven (Must)

### Phase 2
- FR-006: Timeout operacji (Should)
- Lepsze parsowanie stack trace'ów
- Wsparcie dla Windows (mvnw.cmd)

### Future Considerations
- Obsługa multi-module (reactor, `-pl`)
- Integracja jako Maven plugin (zamiast wrappera CLI) — patrz Open Question #1
- Tool do odczytu drzewa zależności (`mvn dependency:tree`)
- Tool do walidacji POM (`mvn validate`)
- Obsługa profili Maven jako dedykowany parametr

## 12. Open Questions

| # | Question | Owner | Due Date |
|---|----------|-------|----------|
| 1 | **RESOLVED:** Wrapper CLI w Javie. Uruchamiamy Maven jako zewnętrzny proces (`ProcessBuilder`), parsujemy stdout/stderr. Java MCP SDK (v0.17+) ze wsparciem dla stdio. W przyszłości możliwa ewolucja do Maven Embedder (in-process) bez zmiany interfejsu MCP. | — | Resolved |
| 2 | Jaki format stack trace'ów w wynikach testów? Pełny stack trace może być bardzo długi — czy przycinać do N linii, czy zwracać cały? | Architect | Phase 1 |
| 3 | Czy serwer powinien cache'ować wynik wykrywania `mvnw` vs `mvn`, czy sprawdzać przy każdym wywołaniu? | Architect | Phase 1 |
| 4 | Czy warto dodać tool `maven_resolve` do samego sprawdzenia/pobrania zależności bez kompilacji? | Stakeholder | Phase 2 |

## 13. Glossary

| Term | Definition |
|------|------------|
| MCP | Model Context Protocol — otwarty protokół komunikacji między AI agentami a narzędziami |
| Tool (MCP) | Funkcja udostępniana przez serwer MCP, wywoływalna przez agenta AI |
| Maven Wrapper (mvnw) | Skrypt zapewniający konkretną wersję Mavena bez konieczności globalnej instalacji |
| Reactor | Mechanizm Maven do budowania wielu modułów w określonej kolejności |
| stdio | Standard input/output — transport MCP, w którym komunikacja odbywa się przez stdin/stdout procesu |
| Claude Code | CLI Anthropic do interakcji z modelem Claude, obsługujący serwery MCP |

## 14. Appendices

### A. Przykładowa konfiguracja Claude Code

```json
{
  "mcpServers": {
    "maven": {
      "command": "java",
      "args": ["-jar", "/path/to/maven-mcp-server.jar", "--project", "/path/to/my-project"],
      "env": {}
    }
  }
}
```

### B. Przykładowe odpowiedzi toolów

**Kompilacja z błędami:**
```json
{
  "status": "FAILURE",
  "duration": 4200,
  "errors": [
    {
      "file": "src/main/java/com/example/MyService.java",
      "line": 42,
      "column": 15,
      "message": "cannot find symbol: variable foo",
      "severity": "ERROR"
    }
  ]
}
```

**Testy z failure:**
```json
{
  "status": "FAILURE",
  "duration": 12000,
  "summary": {
    "testsRun": 15,
    "testsFailed": 2,
    "testsSkipped": 1,
    "testsErrored": 0
  },
  "failures": [
    {
      "testClass": "com.example.MyServiceTest",
      "testMethod": "shouldReturnUser",
      "message": "expected:<200> but was:<404>",
      "stackTrace": "..."
    }
  ]
}
```
