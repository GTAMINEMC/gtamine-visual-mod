# Определение GTAMINE Visual из Paper/Bukkit

Мод объявляет стандартный канал **`gtamine:visual`** при подключении к разрешённому
серверу. Проверка использует только Bukkit API: библиотеки, дополнительные плагины
и собственные сообщения не нужны. Версия мода не передаётся.

## Проверка одной строкой

```java
boolean hasMod = player.getListeningPluginChannels().contains("gtamine:visual");
```

`true` означает, что клиент объявил канал GTAMINE Visual в текущем подключении.
Сразу в `PlayerJoinEvent` канал ещё может отсутствовать: используйте событие
регистрации, если результат нужен сразу после подключения.

## Обнаружение при регистрации канала

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class VisualIntegration extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);

        // Учитываем игроков, которые уже онлайн при включении плагина.
        for (var player : getServer().getOnlinePlayers()) {
            if (player.getListeningPluginChannels().contains("gtamine:visual")) {
                getLogger().info(player.getName() + " использует GTAMINE Visual");
            }
        }
    }

    @EventHandler
    public void onRegisterChannel(PlayerRegisterChannelEvent event) {
        if (event.getChannel().equals("gtamine:visual")) {
            getLogger().info(event.getPlayer().getName() + " использует GTAMINE Visual");
            // Здесь можно включить интеграцию для игрока.
        }
    }
}
```

Серверному плагину не требуется регистрировать входящий или исходящий канал:
он только читает объявленные клиентом каналы. Отправлять запросы не нужно.

## Поведение

- Канал объявляется только для `gtamine.com`, `gtamine.ru`, `needmine.ru`,
  `playgo.pro` и любых их субдоменов. Проверяется адрес подключения игрока,
  а не IP или результат SRV.
- На других серверах и в одиночной игре мод не объявляет канал.
- `false` означает отсутствие объявленного канала на момент проверки;
  регистрация ещё могла не прийти.
- Если сохраняете собственный статус игрока, обрабатывайте также
  `PlayerUnregisterChannelEvent` и `PlayerQuitEvent`. При прямой проверке
  через `getListeningPluginChannels()` собственный кеш не нужен.
- Прокси должен пропускать объявления каналов `minecraft:register` и
  `minecraft:unregister`. Проверяйте это в своей конфигурации прокси.
- Канал служит только признаком наличия мода. Версии, настройки HUD и другие
  пользовательские данные через него не передаются. Входящие тела сообщений
  игнорируются, ответов на них нет.
- Объявление канала может быть воспроизведено другим клиентом: это средство
  совместимости, а не подтверждение подлинности или античит.

Прежняя API с запросами, ответами и получением версии удалена. Для интеграции
используйте стандартные методы и события Bukkit из примеров выше.
