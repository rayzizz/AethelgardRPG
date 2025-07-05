package me.ray.aethelgardRPG.core.configs;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import me.ray.aethelgardRPG.AethelgardRPG;
import me.ray.aethelgardRPG.modules.character.CharacterData;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class LanguageManager {
  private final AethelgardRPG plugin;
  private final Map<String, Map<String, FileConfiguration>> loadedLanguages;
  private String defaultLang;

  // --- INÍCIO DA ALTERAÇÃO ---
  // Cache temporário para o idioma do jogador, usado antes do CharacterData completo ser carregado.
  private final Map<UUID, String> playerLanguageCache;
  // --- FIM DA ALTERAÇÃO ---

  public LanguageManager(AethelgardRPG plugin) {
    this.plugin = plugin;
    this.loadedLanguages = new HashMap<>();
    this.playerLanguageCache = new ConcurrentHashMap<>(); // Inicializa o cache
    this.defaultLang = "pt_br"; // Initial default, will be overridden by config
  }

  public void loadLanguages() {
    this.loadedLanguages.clear();
    this.playerLanguageCache.clear(); // Limpa o cache ao recarregar
    this.defaultLang = this.plugin.getConfigManager().getMainConfig().getString("general.default-language", "pt_br");
    this.plugin.getLogger().info("Carregando idiomas. Idioma padrão do servidor: " + this.defaultLang);

    File pluginLanguagesDataDir = new File(this.plugin.getDataFolder(), "languages");
    if (!pluginLanguagesDataDir.exists() &&
            !pluginLanguagesDataDir.mkdirs()) {
      this.plugin.getLogger().severe("Não foi possível criar o diretório 'languages' na pasta de dados do plugin.");
      return;
    }

    extractDefaultLanguagesFromJar(pluginLanguagesDataDir);

    loadLanguagesFromDataFolder(pluginLanguagesDataDir);

    if (!this.loadedLanguages.containsKey(this.defaultLang))
      this.plugin.getLogger().warning("O idioma padrão do servidor '" + this.defaultLang + "' não foi encontrado ou não pôde ser carregado. Verifique as configurações e arquivos de idioma.");

    long totalComponentFiles = this.loadedLanguages.values().stream().mapToLong(Map::size).sum();
    this.plugin.getLogger().info(this.loadedLanguages.size() + " idiomas carregados. " + totalComponentFiles + " arquivos de componentes no total.");

    validateLanguageFiles();
  }

  private void extractDefaultLanguagesFromJar(File pluginLanguagesDataDir) {
    String jarResourcePrefix = "languages/";
    try {
      CodeSource src = AethelgardRPG.class.getProtectionDomain().getCodeSource();
      if (src != null) {
        URL jarUrl = src.getLocation();
        try (ZipInputStream zip = new ZipInputStream(jarUrl.openStream())) {
          ZipEntry ze;
          while ((ze = zip.getNextEntry()) != null) {
            String entryName = ze.getName();
            if (entryName.startsWith(jarResourcePrefix) && entryName.endsWith(".yml") && !ze.isDirectory()) {
              File targetFile = new File(this.plugin.getDataFolder(), entryName);
              if (!targetFile.getParentFile().exists())
                targetFile.getParentFile().mkdirs();
              if (!targetFile.exists()) {
                this.plugin.saveResource(entryName, false);
                this.plugin.getLogger().info("Arquivo de idioma padrão extraído do JAR: " + entryName);
              }
            }
          }
        }
      } else {
        this.plugin.getLogger().warning("Não foi possível obter CodeSource para escanear arquivos de idioma padrão no JAR.");
      }
    } catch (IOException e) {
      this.plugin.getLogger().log(Level.SEVERE, "Erro ao escanear o JAR por arquivos de idioma padrão.", e);
    }
  }

  private void loadLanguagesFromDataFolder(File pluginLanguagesDataDir) {
    File[] langDirs = pluginLanguagesDataDir.listFiles(File::isDirectory);
    if (langDirs == null) {
      this.plugin.getLogger().warning("Não foi possível listar os diretórios de idioma em " + pluginLanguagesDataDir.getPath());
      return;
    }

    for (File langDir : langDirs) {
      String langCode = langDir.getName().toLowerCase();
      Map<String, FileConfiguration> componentsForLang = new HashMap<>();
      loadComponentsFromDiskRecursive(langDir, langDir, componentsForLang);
      if (!componentsForLang.isEmpty()) {
        this.loadedLanguages.put(langCode, componentsForLang);
        this.plugin.getLogger().info("Idioma '" + langCode + "' carregado com " + componentsForLang.size() + " componentes.");
      } else {
        this.plugin.getLogger().warning("Nenhum componente de idioma encontrado para '" + langCode + "' em " + langDir.getPath());
      }
    }
  }

  private void loadComponentsFromDiskRecursive(File currentDir, File langRootDir, Map<String, FileConfiguration> componentsMap) {
    File[] files = currentDir.listFiles();
    if (files == null)
      return;
    for (File file : files) {
      if (file.isDirectory()) {
        loadComponentsFromDiskRecursive(file, langRootDir, componentsMap);
      } else if (file.getName().endsWith(".yml")) {
        try {
          String relativePath = langRootDir.toURI().relativize(file.toURI()).getPath();
          String componentKey = relativePath.replace(".yml", "").replace('/', '.').toLowerCase();
          FileConfiguration langFileConfig = YamlConfiguration.loadConfiguration(file);
          componentsMap.put(componentKey, langFileConfig);
          this.plugin.getLogger().fine("Componente '" + componentKey + "' carregado do arquivo: " + file.getPath());
        } catch (Exception e) {
          this.plugin.getLogger().log(Level.SEVERE, "Erro ao carregar componente de idioma do arquivo: " + file.getPath(), e);
        }
      }
    }
  }

  public String getDefaultLang() {
    return this.defaultLang;
  }

  /**
   * Retorna um conjunto de todos os códigos de idioma carregados.
   * @return Um Set contendo os códigos de idioma (ex: "pt_br", "en_us").
   */
  public Set<String> getLoadedLanguages() {
    return this.loadedLanguages.keySet();
  }

  // --- INÍCIO DA ALTERAÇÃO ---
  /**
   * Define o idioma preferido de um jogador no cache temporário.
   * Isso é usado principalmente ao entrar no servidor, antes que o CharacterData completo seja carregado.
   */
  public void setPlayerLanguage(Player player, String langCode) {
    if (player == null || langCode == null) return;
    playerLanguageCache.put(player.getUniqueId(), langCode.toLowerCase());
    this.plugin.getLogger().fine("Idioma do jogador " + player.getName() + " definido no cache para " + langCode);
  }

  /**
   * Limpa o idioma em cache para um jogador.
   * Deve ser chamado quando o jogador sai ou quando seu CharacterData completo é carregado na sessão.
   */
  public void clearPlayerLanguageCache(Player player) {
    if (player == null) return;
    if (playerLanguageCache.remove(player.getUniqueId()) != null) {
      this.plugin.getLogger().fine("Cache de idioma limpo para o jogador " + player.getName());
    }
  }
  // --- FIM DA ALTERAÇÃO ---

  public String getServerMessage(String fullKey, Object... args) {
    return internalRetrieveMessage(this.defaultLang, fullKey, args);
  }

  public List<String> getServerMessages(String fullKey, Object... args) {
    return internalRetrieveMessages(this.defaultLang, fullKey, args);
  }

  public String getMessage(Player player, String fullKey, Object... args) {
    String langCodeToUse = this.defaultLang;

    // --- INÍCIO DA ALTERAÇÃO ---
    // 1. Verifica o cache temporário primeiro.
    String cachedLang = playerLanguageCache.get(player.getUniqueId());
    if (cachedLang != null && this.loadedLanguages.containsKey(cachedLang)) {
      langCodeToUse = cachedLang;
    } else {
      // 2. Se não estiver no cache, verifica o CharacterData ativo (comportamento original).
      Optional<CharacterData> playerDataOpt = this.plugin.getCharacterData(player);
      if (playerDataOpt.isPresent()) {
        String playerPref = playerDataOpt.get().getLanguagePreference();
        if (playerPref != null && this.loadedLanguages.containsKey(playerPref.toLowerCase())) {
          langCodeToUse = playerPref.toLowerCase();
        }
      }
    }
    // --- FIM DA ALTERAÇÃO ---

    String message = internalRetrieveMessage(langCodeToUse, fullKey, args);

    if (message.startsWith(ChatColor.RED + "Mensagem ausente:") && !langCodeToUse.equals(this.defaultLang)) {
      this.plugin.getLogger().fine("Chave '" + fullKey + "' não encontrada no idioma preferido do jogador '" + langCodeToUse + "'. Tentando idioma padrão '" + this.defaultLang + "'.");
      message = internalRetrieveMessage(this.defaultLang, fullKey, args);
    }
    return message;
  }

  public List<String> getMessages(Player player, String fullKey, Object... args) {
    String langCodeToUse = this.defaultLang;

    // --- INÍCIO DA ALTERAÇÃO ---
    // 1. Verifica o cache temporário primeiro.
    String cachedLang = playerLanguageCache.get(player.getUniqueId());
    if (cachedLang != null && this.loadedLanguages.containsKey(cachedLang)) {
      langCodeToUse = cachedLang;
    } else {
      // 2. Se não estiver no cache, verifica o CharacterData ativo (comportamento original).
      Optional<CharacterData> playerDataOpt = this.plugin.getCharacterData(player);
      if (playerDataOpt.isPresent()) {
        String playerPref = playerDataOpt.get().getLanguagePreference();
        if (playerPref != null && this.loadedLanguages.containsKey(playerPref.toLowerCase())) {
          langCodeToUse = playerPref.toLowerCase();
        }
      }
    }
    // --- FIM DA ALTERAÇÃO ---

    List<String> messages = internalRetrieveMessages(langCodeToUse, fullKey, args);
    if (messages.size() == 1 && messages.get(0).startsWith(ChatColor.RED + "Lista ausente:") && !langCodeToUse.equals(this.defaultLang)) {
      this.plugin.getLogger().fine("Lista de chaves '" + fullKey + "' não encontrada no idioma preferido do jogador '" + langCodeToUse + "'. Tentando idioma padrão '" + this.defaultLang + "'.");
      messages = internalRetrieveMessages(this.defaultLang, fullKey, args);
    }
    return messages;
  }

  private String internalRetrieveMessage(String langCode, String fullKey, Object... args) {
    Map<String, FileConfiguration> componentsForLang = this.loadedLanguages.get(langCode);
    if (componentsForLang == null || componentsForLang.isEmpty()) {
      this.plugin.getLogger().warning("Idioma '" + langCode + "' não carregado ou sem componentes. Não é possível obter a chave: " + fullKey);
      return ChatColor.RED + "Idioma não carregado: " + langCode + " para chave: " + fullKey;
    }

    this.plugin.getLogger().fine("[LanguageManager] Solicitando mensagem para lang: '" + langCode + "', fullKey: '" + fullKey + "'. Componentes carregados para este idioma: " + componentsForLang.keySet());

    String bestMatchComponentPrefix = null;
    FileConfiguration bestMatchConfig = null;
    String bestMatchKeyInFile = null;

    for (Map.Entry<String, FileConfiguration> entry : componentsForLang.entrySet()) {
      String currentComponentPrefix = entry.getKey();
      if (fullKey.startsWith(currentComponentPrefix + ".")) {
        String currentKeyInFile = fullKey.substring((currentComponentPrefix + ".").length());
        if (entry.getValue().isString(currentKeyInFile)) {
          if (bestMatchComponentPrefix == null || currentComponentPrefix.length() > bestMatchComponentPrefix.length()) {
            bestMatchComponentPrefix = currentComponentPrefix;
            bestMatchConfig = entry.getValue();
            bestMatchKeyInFile = currentKeyInFile;
          }
        }
      }
    }

    if (bestMatchConfig != null) {
      this.plugin.getLogger().finer("[LanguageManager] Chave '" + bestMatchKeyInFile + "' encontrada no componente mais específico '" + bestMatchComponentPrefix + "'.");
      String message = bestMatchConfig.getString(bestMatchKeyInFile);
      message = ChatColor.translateAlternateColorCodes('&', message);
      return MessageFormat.format(message, args);
    }


    if (!fullKey.contains(".")) {
      FileConfiguration directFileConfig = componentsForLang.get(fullKey.toLowerCase());
      if (directFileConfig != null && directFileConfig.isString(fullKey)) {
        this.plugin.getLogger().finer("[LanguageManager] Chave '" + fullKey + "' encontrada no componente '" + fullKey.toLowerCase() + "' (match direto nome do arquivo/chave).");
        String message = directFileConfig.getString(fullKey);
        message = ChatColor.translateAlternateColorCodes('&', message);
        return MessageFormat.format(message, args);
      }
      this.plugin.getLogger().finer("[LanguageManager] Tentativa de match direto para '" + fullKey + "' falhou ou não é uma string.");
    }

    this.plugin.getLogger().warning("Tradução ausente para a chave '" + fullKey + "' no idioma '" + langCode + "'.");
    return ChatColor.RED + "Mensagem ausente: " + fullKey + " (lang: " + langCode + ")";
  }

  private List<String> internalRetrieveMessages(String langCode, String fullKey, Object... args) {
    Map<String, FileConfiguration> componentsForLang = this.loadedLanguages.get(langCode);
    if (componentsForLang == null || componentsForLang.isEmpty()) {
      this.plugin.getLogger().warning("Idioma '" + langCode + "' não carregado. Não é possível obter a lista de chaves: " + fullKey);
      return List.of(ChatColor.RED + "Lista ausente: " + langCode + " para lista: " + fullKey);
    }

    this.plugin.getLogger().fine("[LanguageManager] Solicitando lista de mensagens para lang: '" + langCode + "', fullKey: '" + fullKey + "'. Componentes carregados para este idioma: " + componentsForLang.keySet());

    String bestMatchComponentPrefix = null;
    FileConfiguration bestMatchConfig = null;
    String bestMatchKeyInFile = null;

    for (Map.Entry<String, FileConfiguration> entry : componentsForLang.entrySet()) {
      String currentComponentPrefix = entry.getKey();
      if (fullKey.startsWith(currentComponentPrefix + ".")) {
        String currentKeyInFile = fullKey.substring((currentComponentPrefix + ".").length());
        if (entry.getValue().isList(currentKeyInFile)) {
          if (bestMatchComponentPrefix == null || currentComponentPrefix.length() > bestMatchComponentPrefix.length()) {
            bestMatchComponentPrefix = currentComponentPrefix;
            bestMatchConfig = entry.getValue();
            bestMatchKeyInFile = currentKeyInFile;
          }
        }
      }
    }

    if (bestMatchConfig != null) {
      this.plugin.getLogger().finer("[LanguageManager] Lista de chaves '" + bestMatchKeyInFile + "' encontrada no componente mais específico '" + bestMatchComponentPrefix + "'.");
      return bestMatchConfig.getStringList(bestMatchKeyInFile).stream()
              .map(line -> ChatColor.translateAlternateColorCodes('&', MessageFormat.format(line, args)))
              .collect(Collectors.toList());
    }

    if (!fullKey.contains(".")) {
      FileConfiguration directFileConfig = componentsForLang.get(fullKey.toLowerCase());
      if (directFileConfig != null && directFileConfig.isList(fullKey)) {
        this.plugin.getLogger().finer("[LanguageManager] Lista de chaves '" + fullKey + "' encontrada no componente '" + fullKey.toLowerCase() + "' (match direto nome do arquivo/chave).");
        return directFileConfig.getStringList(fullKey).stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', MessageFormat.format(line, args)))
                .collect(Collectors.toList());
      }
      this.plugin.getLogger().finer("[LanguageManager] Tentativa de match direto para lista '" + fullKey + "' falhou ou não é uma lista.");
    }

    this.plugin.getLogger().warning("Lista de mensagens ausente para a chave '" + fullKey + "' no idioma '" + langCode + "'.");
    return List.of(ChatColor.RED + "Lista ausente: " + fullKey + " (lang: " + langCode + ")");
  }

  public void reloadLanguages() {
    this.loadedLanguages.clear();
    loadLanguages();
    this.plugin.getLogger().info("Arquivos de idioma recarregados.");
  }

  private void validateLanguageFiles() {
    this.plugin.getLogger().info("Iniciando validação dos arquivos de idioma...");

    Map<String, FileConfiguration> defaultComponents = this.loadedLanguages.get(this.defaultLang);
    if (defaultComponents == null || defaultComponents.isEmpty()) {
      this.plugin.getLogger().warning("Não foi possível validar os idiomas: o idioma padrão '" + this.defaultLang + "' não foi carregado ou está vazio.");
      return;
    }

    Set<String> defaultKeys = new HashSet<>();
    for (Map.Entry<String, FileConfiguration> entry : defaultComponents.entrySet()) {
      String componentPrefix = entry.getKey();
      FileConfiguration config = entry.getValue();
      for (String keyInFile : config.getKeys(true)) {
        if (!config.isConfigurationSection(keyInFile)) {
          defaultKeys.add(componentPrefix + "." + keyInFile);
        }
      }
    }
    this.plugin.getLogger().info("Total de chaves de mensagem no idioma padrão ('" + this.defaultLang + "'): " + defaultKeys.size());

    for (Map.Entry<String, Map<String, FileConfiguration>> langEntry : this.loadedLanguages.entrySet()) {
      String currentLangCode = langEntry.getKey();
      if (currentLangCode.equals(this.defaultLang)) {
        continue;
      }

      this.plugin.getLogger().info("--- Validando idioma: '" + currentLangCode + "' ---");
      Map<String, FileConfiguration> currentComponents = langEntry.getValue();
      Set<String> currentLangKeys = new HashSet<>();
      for (Map.Entry<String, FileConfiguration> componentEntry : currentComponents.entrySet()) {
        String componentPrefix = componentEntry.getKey();
        FileConfiguration config = componentEntry.getValue();
        for (String keyInFile : config.getKeys(true)) {
          if (!config.isConfigurationSection(keyInFile)) {
            currentLangKeys.add(componentPrefix + "." + keyInFile);
          }
        }
      }

      Set<String> missingKeys = new HashSet<>(defaultKeys);
      missingKeys.removeAll(currentLangKeys);
      if (!missingKeys.isEmpty()) {
        this.plugin.getLogger().warning("As seguintes chaves estão FALTANDO no idioma '" + currentLangCode + "':");
        missingKeys.forEach(key -> this.plugin.getLogger().warning("  - " + key));
      }

      Set<String> orphanedKeys = new HashSet<>(currentLangKeys);
      orphanedKeys.removeAll(defaultKeys);
      if (!orphanedKeys.isEmpty()) {
        this.plugin.getLogger().warning("As seguintes chaves ÓRFÃS existem em '" + currentLangCode + "' mas não no idioma padrão (podem ser removidas):");
        orphanedKeys.forEach(key -> this.plugin.getLogger().warning("  - " + key));
      }

      Set<String> commonKeys = new HashSet<>(currentLangKeys);
      commonKeys.retainAll(defaultKeys);
      int placeholderCount = 0;
      for (String commonKey : commonKeys) {
        String bestMatchComponentPrefix = null;
        for (String componentPrefix : currentComponents.keySet()) {
          if (commonKey.startsWith(componentPrefix + ".")) {
            if (bestMatchComponentPrefix == null || componentPrefix.length() > bestMatchComponentPrefix.length()) {
              bestMatchComponentPrefix = componentPrefix;
            }
          }
        }

        if (bestMatchComponentPrefix != null) {
          String keyInFile = commonKey.substring((bestMatchComponentPrefix + ".").length());
          FileConfiguration config = currentComponents.get(bestMatchComponentPrefix);

          if (config.isString(keyInFile)) {
            String value = config.getString(keyInFile, "").trim();
            if (value.isEmpty() || value.equalsIgnoreCase("TODO")) {
              this.plugin.getLogger().warning("  -> Placeholder/Vazio: A chave '" + commonKey + "' em '" + currentLangCode + "' tem um valor vazio ou é um placeholder.");
              placeholderCount++;
            }
          } else if (config.isList(keyInFile)) {
            List<String> values = config.getStringList(keyInFile);
            if (values.isEmpty() || (values.size() == 1 && values.get(0).trim().isEmpty())) {
              this.plugin.getLogger().warning("  -> Placeholder/Vazio: A lista de chaves '" + commonKey + "' em '" + currentLangCode + "' está vazia.");
              placeholderCount++;
            }
          }
        }
      }

      if (missingKeys.isEmpty() && orphanedKeys.isEmpty() && placeholderCount == 0) {
        this.plugin.getLogger().info("Idioma '" + currentLangCode + "' está completo e sincronizado com o idioma padrão.");
      }
    }
    this.plugin.getLogger().info("Validação dos arquivos de idioma concluída.");
  }
}