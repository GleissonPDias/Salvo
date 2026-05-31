# 📐 Fluxogramas do Projeto Salvô

> **Versão:** 1.0 &nbsp;|&nbsp; **Data:** 31/05/2026

Este documento contém todos os fluxogramas e diagramas do projeto Salvô, organizados por funcionalidade.

---

## 📑 Índice

1. [Fluxograma Geral — Navegação Completa](#1-fluxograma-geral--navegação-completa)
2. [Fluxo de Autenticação](#2-fluxo-de-autenticação)
3. [Fluxo de Socorro Mecânico (Tempo Real)](#3-fluxo-de-socorro-mecânico-tempo-real)
4. [Fluxo do Dashboard do Prestador](#4-fluxo-do-dashboard-do-prestador)
5. [Fluxo de Gestão de Serviços](#5-fluxo-de-gestão-de-serviços)
6. [Fluxo de Gestão de Veículos](#6-fluxo-de-gestão-de-veículos)
7. [Diagrama de Sequência — Socorro Completo](#7-diagrama-de-sequência--socorro-completo)
8. [Mapa de Endpoints da API](#8-mapa-de-endpoints-da-api)
9. [Diagrama de Entidade-Relacionamento (Models)](#9-diagrama-de-entidade-relacionamento-models)

---

## 1. Fluxograma Geral — Navegação Completa

Este fluxograma mostra **todas as 18 telas** do aplicativo e como elas se conectam:

```mermaid
flowchart TD
    START(("🚀 App Inicia"))
    
    START --> LOGIN

    subgraph AUTH["🔐 AUTENTICAÇÃO"]
        LOGIN["LoginActivity<br/>📧 Email + Senha"]
        CHOOSE["RegisterChooseActivity<br/>🔀 Escolha de Perfil"]
        REG_CLI["RegisterActivity<br/>📝 Cadastro Cliente<br/>Nome, Email, CPF, Tel, Senha"]
        REG_MEC["RegisterMecActivity<br/>🏢 Cadastro Prestador<br/>Razão Social, CNPJ, Tel + GPS"]
    end
    
    subgraph CLIENTE["👤 ÁREA DO CLIENTE"]
        MAIN["MainScreenActivity<br/>🗺️ Mapa + Localização<br/>Botões: Guincho, Bateria, Pneu, Mecânica"]
        SOCORRO["SocorroActivity<br/>🆘 Pedido de Socorro<br/>GPS + Polling 5s"]
        PEDIDOS_CLI["MeusPedidosActivity<br/>📋 Meus Pedidos<br/>Abas: Andamento | Concluídos<br/>Polling automático 5s"]
        VEICULOS_CLI["MeusVeiculosActivity<br/>🚗 Meus Veículos<br/>CRUD + Foto Base64"]
        PERFIL_CLI["PerfilClienteActivity<br/>👤 Meu Perfil"]
        AVALIACAO["AvaliacaoActivity<br/>⭐ Avaliação<br/>Stars + Chips + Comentário"]
    end
    
    subgraph PRESTADOR["🏪 ÁREA DO PRESTADOR"]
        HOME["HomePrestadorActivity<br/>📡 Dashboard / Radar<br/>WebSocket + Slide-to-Accept<br/>Métricas + Atividades Recentes"]
        CARDAPIO["CardapioServicosActivity<br/>🛠️ Cardápio de Serviços<br/>Preço Fixo | Preço por KM"]
        FROTA["GestaoFrotaActivity<br/>🚛 Gestão da Frota<br/>CRUD + Status Operacional"]
        STATUS["StatusVeiculoActivity<br/>🔍 Status do Veículo<br/>Detalhes + Gestão"]
        PEDIDOS_OF["MeusPedidosOficinaActivity<br/>📑 Pedidos da Oficina<br/>Abas: Ativos | Histórico"]
        DETALHES["DetalhesPedidoOficinaActivity<br/>📄 Detalhes do Pedido<br/>Geocoder Reverso"]
        PERFIL_OF["PerfilOficinaActivity<br/>🏪 Perfil da Oficina<br/>Banner + Fotos + Places"]
    end
    
    SOBRE["SobreActivity<br/>ℹ️ Sobre o App"]

    %% Auth Flow
    LOGIN -->|"Cadastro"| CHOOSE
    LOGIN -->|"Auto-login<br/>role=customer"| MAIN
    LOGIN -->|"Auto-login<br/>role=provider"| HOME
    LOGIN -->|"Login OK<br/>role=customer"| MAIN
    LOGIN -->|"Login OK<br/>role=provider"| HOME
    CHOOSE -->|"Cliente"| REG_CLI
    CHOOSE -->|"Empresa"| REG_MEC
    REG_CLI -->|"Sucesso<br/>após 2s"| LOGIN
    REG_MEC -->|"Sucesso<br/>após 2s"| LOGIN

    %% Cliente Flow
    MAIN -->|"Guincho/Bateria<br/>Pneu/Mecânica"| SOCORRO
    MAIN -->|"Bottom Nav<br/>PEDIDOS"| PEDIDOS_CLI
    MAIN -->|"Bottom Nav<br/>PERFIL"| PERFIL_CLI
    SOCORRO -->|"Aceito pela<br/>oficina"| PEDIDOS_CLI
    PERFIL_CLI -->|"Meus Veículos"| VEICULOS_CLI
    PERFIL_CLI -->|"Histórico"| PEDIDOS_CLI
    PERFIL_CLI -->|"Sobre"| SOBRE
    PERFIL_CLI -->|"Logout"| LOGIN

    %% Prestador Flow
    HOME -->|"Bottom Nav<br/>SERVIÇOS"| CARDAPIO
    HOME -->|"Bottom Nav<br/>FROTA"| FROTA
    HOME -->|"Bottom Nav<br/>PERFIL"| PERFIL_OF
    HOME -->|"Histórico<br/>Pedidos"| PEDIDOS_OF
    FROTA -->|"Clique no<br/>Veículo"| STATUS
    PEDIDOS_OF -->|"Clique no<br/>Pedido"| DETALHES
    PERFIL_OF -->|"Ver Serviços"| CARDAPIO
    PERFIL_OF -->|"Ver Veículos"| FROTA
    PERFIL_OF -->|"Logout"| LOGIN

    %% Styling
    style AUTH fill:#1E293B,stroke:#F59E0B,color:#fff
    style CLIENTE fill:#0F172A,stroke:#00E5FF,color:#fff
    style PRESTADOR fill:#0F172A,stroke:#FF8C00,color:#fff
    style LOGIN fill:#F59E0B,stroke:#000,color:#000
    style MAIN fill:#00E5FF,stroke:#000,color:#000
    style HOME fill:#FF8C00,stroke:#000,color:#000
    style SOCORRO fill:#EF4444,stroke:#fff,color:#fff
```

---

## 2. Fluxo de Autenticação

### 2.1 Login Completo

```mermaid
flowchart TD
    A(("App Inicia")) --> B{"SessionManager<br/>userId ≠ -1?"}
    
    B -->|"Sim (sessão salva)"| C{"Qual role?"}
    C -->|"customer"| D["→ MainScreenActivity"]
    C -->|"provider"| E["→ HomePrestadorActivity"]
    
    B -->|"Não (sem sessão)"| F["Exibe tela de Login"]
    F --> G["Usuário preenche<br/>Email + Senha"]
    G --> H["Clica 'Entrar'"]
    H --> I{"Campos<br/>vazios?"}
    I -->|"Sim"| J["❌ Toast: Preencha todos os campos"]
    J --> G
    
    I -->|"Não"| K["POST /login<br/>LoginRequest"]
    K --> L{"Resposta<br/>da API"}
    
    L -->|"sucesso = true"| M["SessionManager<br/>salvarSessao(userId, role, nome)"]
    M --> N{"role?"}
    N -->|"customer"| D
    N -->|"provider"| E
    
    L -->|"sucesso = false"| O["❌ Toast: mensagem de erro"]
    O --> G
    
    L -->|"Falha de rede"| P["❌ Toast: Erro de conexão"]
    P --> G
    
    style D fill:#00E5FF,stroke:#000,color:#000
    style E fill:#FF8C00,stroke:#000,color:#000
    style J fill:#EF4444,color:#fff
    style O fill:#EF4444,color:#fff
    style P fill:#EF4444,color:#fff
```

### 2.2 Cadastro

```mermaid
flowchart TD
    A["RegisterChooseActivity"] --> B{"Tipo de<br/>cadastro?"}
    
    B -->|"Cliente"| C["RegisterActivity"]
    B -->|"Empresa"| D["RegisterMecActivity"]
    
    subgraph CLIENTE["Cadastro Cliente"]
        C --> C1["Preencher:<br/>Nome, Email, CPF,<br/>Telefone, Senha, Confirmar"]
        C1 --> C2{"Validação"}
        C2 -->|"Campos vazios"| C3["❌ Mensagem de erro"]
        C3 --> C1
        C2 -->|"Senhas diferentes"| C4["❌ Senhas não coincidem"]
        C4 --> C1
        C2 -->|"OK"| C5["POST /cadastro<br/>lat=0.0, lon=0.0<br/>role=customer"]
    end
    
    subgraph PRESTADOR["Cadastro Prestador"]
        D --> D1["Preencher:<br/>Razão Social, Email,<br/>CNPJ, Telefone, Senha"]
        D1 --> D2{"Permissão<br/>GPS?"}
        D2 -->|"Negada"| D3["❌ Solicita permissão"]
        D3 --> D2
        D2 -->|"Concedida"| D4{"Validação"}
        D4 -->|"Erro"| D5["❌ Mensagem de erro"]
        D5 --> D1
        D4 -->|"OK"| D6["FusedLocation<br/>obter lat/lng"]
        D6 --> D7["POST /cadastro<br/>lat=real, lon=real<br/>role=provider"]
    end
    
    C5 --> E{"Resposta API"}
    D7 --> E
    
    E -->|"sucesso = true"| F["✅ Snackbar verde"]
    F -->|"Após 2s"| G["→ LoginActivity"]
    
    E -->|"sucesso = false"| H["❌ Snackbar com erro"]
    
    style CLIENTE fill:#0a1628,stroke:#00E5FF,color:#fff
    style PRESTADOR fill:#0a1628,stroke:#FF8C00,color:#fff
    style F fill:#10B981,color:#fff
```

---

## 3. Fluxo de Socorro Mecânico (Tempo Real)

Este é o **fluxo principal** do aplicativo:

```mermaid
flowchart TD
    subgraph CLIENTE["📱 LADO DO CLIENTE"]
        A["MainScreenActivity<br/>Vê mapa com localização"] 
        A --> B["Clica em tipo de serviço<br/>🚗 Guincho | 🔋 Bateria<br/>🛞 Pneu | 🔧 Mecânica"]
        B --> C["SocorroActivity"]
        C --> D["Confirma pedido de socorro"]
        D --> E{"Permissão GPS?"}
        E -->|"Não"| F["Solicita permissão"]
        F --> E
        E -->|"Sim"| G["FusedLocation<br/>PRIORITY_HIGH_ACCURACY"]
        G --> H["POST /solicitar-socorro<br/>PedidoSocorroRequest"]
        H --> I{"Resposta API"}
        I -->|"Falha"| J["❌ Toast erro"]
        I -->|"Sucesso"| K["Recebe requestId"]
        K --> L["📡 Inicia Polling<br/>a cada 5 segundos"]
        
        L --> M["GET /status-pedido/requestId"]
        M --> N{"Status?"}
        N -->|"searching"| O["⏳ Aguardando...<br/>Continua polling"]
        O --> L
        N -->|"accepted"| P["✅ Oficina aceitou!"]
        P --> Q["→ MeusPedidosActivity"]
        N -->|"canceled"| R["❌ Cancelado"]
        R --> S["Para polling"]
    end
    
    subgraph SERVIDOR["🌐 SERVIDOR (Ktor/Render)"]
        H2["Recebe pedido"]
        H2 --> H3["Busca oficinas online<br/>próximas ao cliente"]
        H3 --> H4["Envia via WebSocket<br/>para N oficinas"]
    end
    
    subgraph OFICINA["🏪 LADO DA OFICINA"]
        W1["HomePrestadorActivity<br/>Prestador está ONLINE"]
        W1 --> W2["WebSocket recebe<br/>JSON do chamado"]
        W2 --> W3["🔔 Exibe overlay de alerta"]
        W3 --> W4["Dados exibidos:<br/>• Veículo do cliente<br/>• Tipo de defeito<br/>• Preço calculado<br/>• Distância<br/>• Nome + Nota do cliente"]
        W4 --> W5["Seleciona veículo<br/>no Spinner"]
        W5 --> W6{"Decisão?"}
        W6 -->|"Recusar"| W7["Esconde alerta"]
        W6 -->|"Aceitar<br/>(slide >75%)"| W8["POST /aceitar-socorro<br/>AceitarPedidoRequestApp"]
        W8 --> W9{"Resposta?"}
        W9 -->|"Sucesso"| W10["✅ Pedido aceito!<br/>Alerta some"]
        W9 -->|"Falha"| W11["❌ Erro<br/>Alerta permanece"]
    end
    
    H -.->|"REST"| H2
    H4 -.->|"WebSocket"| W2
    W8 -.->|"REST"| H2
    
    style CLIENTE fill:#0a1628,stroke:#00E5FF,color:#fff
    style SERVIDOR fill:#1E293B,stroke:#F59E0B,color:#fff
    style OFICINA fill:#0a1628,stroke:#FF8C00,color:#fff
    style P fill:#10B981,color:#fff
    style R fill:#EF4444,color:#fff
    style W10 fill:#10B981,color:#fff
```

---

## 4. Fluxo do Dashboard do Prestador

```mermaid
flowchart TD
    A["HomePrestadorActivity<br/>Dashboard do Prestador"] --> B["Carrega dados iniciais"]
    
    B --> B1["GET /listar-pedidos-oficina<br/>Histórico + Estatísticas"]
    B --> B2["GET /veiculos-oficina<br/>Frota para Spinner"]
    B --> B3["Configura Google Maps<br/>Marcador + Círculo 1500m"]
    
    B1 --> C["Exibe no Dashboard:<br/>• Nome + Foto da oficina<br/>• Total de ganhos (R$)<br/>• Total de resgates<br/>• 5 atividades recentes"]
    
    B2 --> D["Popula Spinner de veículos<br/>no alerta de socorro"]
    
    subgraph ONLINE["Toggle Online/Offline"]
        E["Switch Status"]
        E -->|"Liga"| F["viewModel.toggleStatus(true)"]
        F --> F1["Atualização Otimista<br/>UI muda imediatamente"]
        F1 --> F2["POST /provider/toggle-status"]
        F2 -->|"Sucesso"| F3["✅ Mantém estado"]
        F2 -->|"Falha"| F4["❌ Reverte estado<br/>Mostra erro"]
        
        E -->|"Desliga"| G["viewModel.toggleStatus(false)"]
        G --> G1["Desconecta WebSocket"]
    end
    
    F3 --> H["Conecta WebSocket<br/>wss://.../{providerId}"]
    H --> I["Aguarda chamados<br/>em tempo real"]
    
    I -->|"Chamado recebido"| J["Exibe Overlay de Alerta<br/>com Slide-to-Accept"]
    
    subgraph BOTTOM_NAV["Bottom Navigation"]
        N1["RADAR<br/>(atual)"]
        N2["SERVIÇOS<br/>→ CardapioServicosActivity"]
        N3["FROTA<br/>→ GestaoFrotaActivity"]
        N4["CHAT<br/>(placeholder)"]
        N5["PERFIL<br/>→ PerfilOficinaActivity"]
    end
    
    subgraph HISTORICO["Histórico / Atividades Recentes"]
        H1["RecyclerView com<br/>RecentActivityAdapter"]
        H1 -->|"Clique no item"| H2["BottomSheetDialog<br/>OrderDetailsDialog"]
        H2 --> H3["Alterar Status"]
        H3 --> H4["OrderStatusDialog<br/>RadioGroup com opções"]
        H4 --> H5["PATCH /atualizar-status-pedido/id"]
    end
    
    style ONLINE fill:#1a2332,stroke:#10B981,color:#fff
    style BOTTOM_NAV fill:#1a2332,stroke:#64748B,color:#fff
    style HISTORICO fill:#1a2332,stroke:#3B82F6,color:#fff
```

---

## 5. Fluxo de Gestão de Serviços

```mermaid
flowchart TD
    A["CardapioServicosActivity"] --> B["GET /servicos-oficina/providerId<br/>Carrega lista de serviços"]
    
    B --> C["Exibe RecyclerView<br/>com ServiceAdapter"]
    
    C --> D["TabLayout filtra"]
    D -->|"Aba Fixo"| E["Filtra: pricePerKm < 0.1"]
    D -->|"Aba KM"| F["Filtra: pricePerKm >= 0.1"]
    
    subgraph ADICIONAR["Adicionar Serviço"]
        G["FAB (+)"] --> H["ServicePriceModeDialog"]
        H -->|"Preço Fixo"| I["BottomSheet<br/>Nome + Preço Fixo"]
        H -->|"Preço por KM"| J["BottomSheet<br/>Nome + Preço KM + Preço Hora"]
        I --> K["POST /adicionar-servico"]
        J --> K
        K --> L["✅ Recarrega lista"]
    end
    
    subgraph GERENCIAR["Gerenciar Serviço"]
        M["Clique no item"] --> N["AlertDialog:<br/>Editar | Excluir"]
        N -->|"Editar"| O["BottomSheet<br/>pré-preenchido"]
        O --> P["PUT /atualizar-servico/id"]
        N -->|"Excluir"| Q["AlertDialog confirmação"]
        Q --> R["DELETE /excluir-servico/id/providerId"]
        
        S["Switch ativo/inativo"] --> T["PATCH /alternar-status-servico/id"]
    end
    
    style ADICIONAR fill:#1a2332,stroke:#10B981,color:#fff
    style GERENCIAR fill:#1a2332,stroke:#F59E0B,color:#fff
```

---

## 6. Fluxo de Gestão de Veículos

### 6.1 Gestão de Frota (Prestador)

```mermaid
flowchart TD
    A["GestaoFrotaActivity"] --> B["GET /veiculos-oficina/providerId"]
    B --> C["RecyclerView<br/>VehicleAdapter"]
    
    C -->|"Toque simples"| D["→ StatusVeiculoActivity<br/>Detalhes do veículo"]
    
    C -->|"Toque longo"| E["AlertDialog:<br/>Alterar Status | Editar"]
    E -->|"Alterar Status"| F["AlertDialog:<br/>Disponível | Em Atendimento | Em Manutenção"]
    F --> G["PATCH /atualizar-status-veiculo/id"]
    E -->|"Editar"| H["BottomSheet com campos<br/>Marca, Nome, Placa, Tipo,<br/>Data Manutenção, Foto"]
    H --> I["PUT /atualizar-veiculo/id"]
    
    C -->|"Swipe"| J["Remove da UI"]
    J --> K["DELETE /excluir-veiculo/id/providerId"]
    
    L["FAB (+)"] --> M["BottomSheet novo veículo<br/>Status default: Disponível"]
    M --> N["POST /adicionar-veiculo"]
    
    subgraph STATUS_VEICULO["StatusVeiculoActivity"]
        D --> D1["Exibe: Modelo, Marca,<br/>Placa, Tipo, Status, Manutenção"]
        D1 --> D2["Card Status Operacional<br/>→ AlertDialog para alterar"]
        D1 --> D3["Card Manutenção<br/>→ AlertDialog para data"]
        D2 --> D4["PATCH /atualizar-status-veiculo/id"]
        D3 --> D5["PUT /atualizar-veiculo/id"]
    end
    
    style STATUS_VEICULO fill:#1a2332,stroke:#3B82F6,color:#fff
```

### 6.2 Veículos do Cliente

```mermaid
flowchart TD
    A["MeusVeiculosActivity"] --> B["GET /veiculos-oficina/customerId"]
    B --> C["RecyclerView"]
    
    C -->|"Clique / Long Click"| D["BottomSheet de Edição<br/>Pré-preenchido"]
    D --> E["PUT /atualizar-veiculo/id"]
    
    C -->|"Swipe"| F["Remove + DELETE"]
    
    G["FAB (+)"] --> H["BottomSheet novo veículo<br/>+ Seleção de Foto"]
    H --> I["uriToBase64(JPEG 70%)"]
    I --> J["POST /adicionar-veiculo"]
```

---

## 7. Diagrama de Sequência — Socorro Completo

```mermaid
sequenceDiagram
    autonumber
    
    participant C as 📱 Cliente
    participant API as 🌐 API Ktor
    participant WS as 🔌 WebSocket
    participant O as 🏪 Oficina
    
    Note over O: Oficina abre o app e fica online
    O->>API: POST /provider/toggle-status (online=true)
    API-->>O: {sucesso: true}
    O->>WS: Conecta wss://.../{providerId}
    WS-->>O: Conexão estabelecida ✅
    
    Note over C: Cliente precisa de socorro
    C->>C: Abre MainScreenActivity (vê mapa)
    C->>C: Clica "Guincho" → SocorroActivity
    C->>C: Obtém GPS (FusedLocation)
    C->>API: POST /solicitar-socorro
    
    Note over API: API processa o pedido
    API->>API: Cria pedido (status=searching)
    API->>API: Busca oficinas online próximas
    API-->>C: {sucesso, requestId, mecanicosNotificados: 3}
    API->>WS: Envia JSON para oficinas próximas
    
    Note over O: Alerta aparece na tela
    WS-->>O: JSON {requestId, veiculo, defeito, preco, distancia, clienteNome}
    O->>O: Exibe overlay de alerta
    O->>O: Seleciona veículo no spinner
    O->>O: Arrasta slider >75%
    
    Note over C: Cliente faz polling enquanto espera
    loop Polling a cada 5s
        C->>API: GET /status-pedido/{requestId}
        API-->>C: {status: "searching"}
    end
    
    Note over O: Oficina aceita o chamado
    O->>API: POST /aceitar-socorro {requestId, providerId, price, distance, vehicleId}
    API->>API: Atualiza pedido (status=accepted)
    API-->>O: {sucesso: true, mensagem: "Pedido aceito"}
    
    Note over C: Polling detecta aceite
    C->>API: GET /status-pedido/{requestId}
    API-->>C: {status: "accepted", detalhesOficina: {...}}
    C->>C: Navega para MeusPedidosActivity ✅
    
    Note over O: Oficina atualiza status durante atendimento
    O->>API: PATCH /atualizar-status-pedido/{id} {status: "en_route"}
    O->>API: PATCH /atualizar-status-pedido/{id} {status: "arrived"}
    O->>API: PATCH /atualizar-status-pedido/{id} {status: "in_progress"}
    O->>API: PATCH /atualizar-status-pedido/{id} {status: "completed"}
    
    Note over C: Cliente acompanha via polling na MeusPedidosActivity
    loop Polling a cada 5s
        C->>API: GET /listar-pedidos?userId={id}
        API-->>C: Lista atualizada com novo status
    end
    
    Note over O: Oficina volta ao radar
    O->>O: Dashboard atualiza métricas
```

---

## 8. Mapa de Endpoints da API

```mermaid
flowchart LR
    subgraph API["🌐 API — apisalvologin.onrender.com"]
        subgraph AUTH_EP["🔐 Autenticação"]
            E1["POST /login"]
            E2["POST /cadastro"]
        end
        
        subgraph SOCORRO_EP["🆘 Socorro"]
            E3["POST /solicitar-socorro"]
            E4["POST /aceitar-socorro"]
            E5["GET /status-pedido/{id}"]
        end
        
        subgraph PEDIDOS_EP["📋 Pedidos"]
            E6["GET /listar-pedidos"]
            E7["GET /listar-pedidos-oficina"]
            E8["PATCH /atualizar-status-pedido/{id}"]
        end
        
        subgraph PERFIL_EP["👤 Perfil"]
            E9["GET /obter-perfil/{id}"]
            E10["PATCH /atualizar-perfil/{id}"]
            E11["POST /provider/toggle-status"]
        end
        
        subgraph SERVICOS_EP["🛠️ Serviços"]
            E12["GET /servicos-oficina/{id}"]
            E13["GET /servicos-publicos/{id}"]
            E14["POST /adicionar-servico"]
            E15["PUT /atualizar-servico/{id}"]
            E16["PATCH /alternar-status-servico/{id}"]
            E17["DELETE /excluir-servico/{id}/{pId}"]
        end
        
        subgraph VEICULOS_EP["🚗 Veículos"]
            E18["GET /veiculos-oficina/{id}"]
            E19["POST /adicionar-veiculo"]
            E20["PUT /atualizar-veiculo/{id}"]
            E21["PATCH /atualizar-status-veiculo/{id}"]
            E22["DELETE /excluir-veiculo/{id}/{pId}"]
        end
        
        subgraph WS_EP["🔌 WebSocket"]
            E23["wss://.../radar-provider/{id}"]
        end
    end
    
    style AUTH_EP fill:#F59E0B,stroke:#000,color:#000
    style SOCORRO_EP fill:#EF4444,stroke:#000,color:#fff
    style PEDIDOS_EP fill:#3B82F6,stroke:#000,color:#fff
    style PERFIL_EP fill:#8B5CF6,stroke:#000,color:#fff
    style SERVICOS_EP fill:#10B981,stroke:#000,color:#fff
    style VEICULOS_EP fill:#00E5FF,stroke:#000,color:#000
    style WS_EP fill:#FF8C00,stroke:#000,color:#000
```

---

## 9. Diagrama de Entidade-Relacionamento (Models)

```mermaid
erDiagram
    USER ||--o{ SERVICE_REQUEST : "faz pedidos"
    USER ||--o{ VEHICLE : "possui veículos"
    USER ||--o{ SERVICE_ITEM : "oferece serviços"
    
    USER {
        int id PK
        string nome
        string email
        string cpf_cnpj
        string telefone
        string password
        string role "customer|provider"
        double latitude
        double longitude
        string user_banner "Base64"
        string foto_1 "Base64"
        string foto_2 "Base64"
        boolean is_online
    }
    
    SERVICE_REQUEST {
        int id PK
        int customer_id FK
        int assigned_provider_id FK
        string service_type
        string description
        string status "searching|accepted|en_route|arrived|in_progress|completed|canceled"
        double final_price
        double final_distance
        string destino_address
        string created_at
        string cliente_nome
        string prestador_nome
        string prestador_foto "Base64"
        string vehicle_info
        string veiculo_prestador_nome
        string veiculo_prestador_placa
        double latitude
        double longitude
    }
    
    VEHICLE {
        int id PK
        int provider_id FK
        string name "modelo"
        string plate "placa"
        string status "Disponível|Em Atendimento|Em Manutenção"
        boolean is_active
        string brand
        string vehicle_type
        string maintenance_date
        string vehicle_photo "Base64"
    }
    
    SERVICE_ITEM {
        int id PK
        int provider_id FK
        string service_type "nome do serviço"
        double base_price
        double price_per_km
        boolean is_active
    }
    
    POLLING_STATUS {
        string status
        string razao_cancelamento
    }
    
    POLLING_STATUS ||--o| OFICINA_DETALHES : "contém"
    
    OFICINA_DETALHES {
        string nome
        string foto_perfil "Base64"
        double valor_final
        double distancia_km
        string nome_veiculo
        string placa_veiculo
    }
    
    SERVICE_REQUEST ||--|| ACEITAR_PEDIDO : "aceito por"
    
    ACEITAR_PEDIDO {
        int request_id FK
        int provider_id FK
        double price
        double distance
        int vehicle_id FK
    }
```

---

## Legenda de Cores dos Fluxogramas

| Cor | Significado |
|-----|------------|
| 🟦 Azul Neon (`#00E5FF`) | Área do Cliente |
| 🟧 Laranja (`#FF8C00`) | Área do Prestador |
| 🟨 Âmbar (`#F59E0B`) | Autenticação |
| 🟩 Verde (`#10B981`) | Sucesso / Confirmação |
| 🟥 Vermelho (`#EF4444`) | Erro / Socorro / Cancelamento |
| 🟪 Roxo (`#8B5CF6`) | Perfil / Status "No Local" |
| ⬜ Cinza (`#64748B`) | Navegação / UI genérica |

---

> **Documento gerado em:** 31/05/2026
> **Projeto:** Salvô v1.0
