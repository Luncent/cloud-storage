## Проектирование классов

- ResourcePathUtil не должен быть инстансом
- Избавится от постоянных вызовов ResourcePathUtil 
- Сделать 1 пакет resource и в него вложить: directory, file, archive
- Подумать куда вынести общий метод getResourceMetadata или оставить