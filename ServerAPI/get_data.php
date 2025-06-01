<?php
header('Content-Type: application/json; charset=utf-8');

// --- Конфигурация базы данных ---
$host = 'localhost';
$username = 'roadmap_user';
$password = '08062023'; // В реальном приложении используйте более надежный пароль и переменные окружения
$database = 'roadmap_db';

// --- Конфигурация директории для загрузки изображений ---
$uploadDirImages = '/var/www/html/uploads_images/';

// --- Инициализация ответа ---
$response = [
    'status' => 'success',
    'message' => 'Данные успешно собраны.',
    'data' => [
        'users' => [],
        'map_points' => [],
        'image_files_on_server' => []
    ]
];

// --- Подключение к базе данных ---
try {
    $pdo = new PDO("mysql:host=$host;dbname=$database;charset=utf8mb4", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION); // Включаем режим обработки ошибок через исключения
} catch (PDOException $e) {
    $response['status'] = 'error';
    $response['message'] = 'Ошибка подключения к базе данных: ' . $e->getMessage();
    echo json_encode($response, JSON_UNESCAPED_UNICODE);
    exit;
}

// --- 1. Сбор данных о пользователях ---
try {
    // ВНИМАНИЕ: НЕ извлекаем поле 'password' для безопасности!
    $stmt_users = $pdo->query("SELECT username, password FROM users");
    $response['data']['users'] = $stmt_users->fetchAll(PDO::FETCH_ASSOC);
    error_log("Данные пользователей успешно извлечены.");
} catch (PDOException $e) {
    $response['status'] = 'warning';
    $response['message'] .= ' Ошибка при сборе данных о пользователях: ' . $e->getMessage();
    error_log("Ошибка при сборе данных о пользователях: " . $e->getMessage());
}

// --- 2. Сбор данных о точках на карте ---
try {
    $stmt_map_points = $pdo->query("SELECT id, userId, label, description, latitude, longitude, photoUris FROM map_points");
    $mapPoints = $stmt_map_points->fetchAll(PDO::FETCH_ASSOC);

    // Декодируем photoUris для каждой точки, так как они хранятся как JSON-строки
    foreach ($mapPoints as &$point) {
        if (isset($point['photoUris']) && is_string($point['photoUris'])) {
            $uris = json_decode($point['photoUris'], true) ?? [];
            $filteredUris = [];
            foreach ($uris as $uri) {
                // Оставляем только URI, которые не начинаются с $uploadDirImages
                if (strpos($uri, $uploadDirImages) !== 0) {
                    $filteredUris[] = $uri;
                }
            }
            $point['photoUris'] = $filteredUris;
        } else {
            $point['photoUris'] = [];
        }
    }
    $response['data']['map_points'] = $mapPoints;
    error_log("Данные точек карты успешно извлечены.");
} catch (PDOException $e) {
    $response['status'] = 'warning';
    $response['message'] .= ' Ошибка при сборе данных о точках карты: ' . $e->getMessage();
    error_log("Ошибка при сборе данных о точках карты: " . $e->getMessage());
}

// --- 3. Сбор списка файлов изображений на сервере ---
$allowedMimeTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']; // Разрешенные типы изображений

if (is_dir($uploadDirImages)) {
    try {
        $files = scandir($uploadDirImages);
        foreach ($files as $file) {
            if ($file === '.' || $file === '..') {
                continue;
            }
            $filePath = $uploadDirImages . $file;
            if (is_file($filePath)) {
                // Проверяем, является ли файл изображением
                $mime_type = mime_content_type($filePath);

                if (in_array($mime_type, $allowedMimeTypes)) {
                    $response['data']['image_files_on_server'][] = $filePath;
                }
            }
        }
        error_log("Список файлов изображений успешно собран.");
    } catch (Exception $e) {
        $response['status'] = 'warning';
        $response['message'] .= ' Ошибка при сканировании директории изображений: ' . $e->getMessage();
        error_log("Ошибка при сканировании директории изображений: " . $e->getMessage());
    }
} else {
    $response['status'] = 'warning';
    $response['message'] .= ' Директория для изображений не найдена: ' . $uploadDirImages;
    error_log("Директория для изображений не найдена: " . $uploadDirImages);
}

// --- Отправка JSON-ответа ---
echo json_encode($response, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT); // JSON_PRETTY_PRINT для читаемости
