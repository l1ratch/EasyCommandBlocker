# Основная информация
**EasyCommandBlocker** - плагин для легкой и удобной блокировки команд на сервере Minecraft<br>
Поддержка версий: `1.19.2 - LATEST`<br>
Поддержка ядер сервера: `Spigot, Paper, другие форки Spigot`<br>
# Права и команды
**1. Права:**<br>
`EasyCommandBlocker.admin` - Доступ к командам плагина (*Доступна оператору, Без обхода*)<br>
`EasyCommandBlocker.bypass-all` - Полный обход всех ограничений (*НЕ доступна оператору*)<br>
`EasyCommandBlocker.bypass-command` - Обход блокировки обычных команд (*НЕ доступна оператору*)<br>
`EasyCommandBlocker.bypass-colon` - Обход блокировки команд с двоеточием (*НЕ доступна оператору*)<br>

**2. Команды:**<br>
`/ecb` или `/ecb help`  - Информация и помощь по плагину<br>
`/ecb reload` - Загрузка изменений конфигурации плагина(без перезагрузки сервера)<br>
`/ecb migrate migrate.yml` - Перенос и адаптация списка команд под конфигурацию<br>
`/ecb cmd add <команда> <'&cтекст блокировки'> <true/false>` - Добавление команды в список<br>
`/ecb cmd edit <команда> <'&cтекст блокировки'> <true/false>` - Изменение параметров команды в списке<br>
`/ecb cmd del <команда>` - Удаление команды из списка<br>

# Пример конфигурации
```
deftxt: '&cЭта команда заблокирована!'  # Текст по умолчанию для заблокированных команд
blockColonCommands: 'true'  # Переключатель для работы с командами с двоеточием

BlockCMD:
  - command: 'op'
    message: '&4Эта команда заблокирована администратором.'
    tabCompleter: true  # Скрыть команду из автозаполнения
  
  - command: 'gm 1'
    message: 'deftxt'  # Использовать текст по умолчанию
    tabCompleter: false  # Команда будет видна в автозаполнении
  
  - command: 'set time 1'
    message: ''  # Не отображать сообщение при блокировке
    tabCompleter: false  # Команда будет видна в автозаполнении
  
  - command: 'gamemode creative'
    message: '&eВы не можете менять режим игры.'
    tabCompleter: true  # Скрыть команду из автозаполнения
```
## Описание параметров конфигурации:
**deftxt:** `'def msg'` Текст по умолчанию, который будет отображаться при блокировке команды.<br>
**blockColonCommands:** `<..>`<br>
     `true` - Команды с двоеточием будут заблокированы и скрыты из автозаполнения.<br>
     `false` - Команды с двоеточием будут доступны и не будут скрыты.<br>
     `HIDE` - Команды с двоеточием будут скрыты из автозаполнения, но останутся доступными.<br>
     `BLOCK` - Команды с двоеточием будут заблокированы, но не скрыты из автозаполнения.<br>
**BlockCMD:**<br>
     command: `'command'` - Команда, которую нужно заблокировать.<br>
     message: `'msg'`/`'deftxt'`/`''` - Сообщение, которое будет отображаться игроку при блокировке команды.<br>
     tabCompleter: `'true'`/`'false'` - Опция для управления видимостью команды в автозаполнении.<br>
