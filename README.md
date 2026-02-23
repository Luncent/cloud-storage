## Структура проекта

- Сделать 1 пакет resource и в него вложить: directory, file, archive
- Разделить resourceService на 2: file & directory  services

## Проектирование классов

- ResourcePathUtil не должен быть инстансом
- Избавится от постоянных вызовов ResourcePathUtil
- Подумать куда вынести общий метод getResourceMetadata или оставить
- createEmptyDirectory перенести в DirectoryService

## По коду 

- storageService.deleteFilesBatch не отлавливаются ошибки 

## Другое

- делать проверку на наличие бакетов в минио при запуске приложения как при миграции
- DirectoryServiceImpl кидать ошибку при notfound
- checkRequestedPathForEmptyDirectoryTag не в StorageService делать 
- StorageServiceImpl формирование ошибок и текстов выходит за границы полномочий этого класса

## ТЗ

- Удаление несуществующих ресурсов возвращает 200