<?php
header('Content-Type: application/json; charset=utf-8');

// Параметры подключения к MySQL
$host = 'localhost';
$username = 'roadmap_user';
$password = '08062023';
$database = 'roadmap_db';

$uploadDirImages = '/var/www/html/uploads_images/';
if (!is_dir($uploadDirImages)) {
    mkdir($uploadDirImages, 0755, true);
}
$response = ['status' => 'success', 'message' => 'Данные успешно обработаны.', 'inserted' => ['users' => 0, 'map_points' => 0], 'updated' => 0, 'uploaded_images' => [], 'updated_users' => 0];
try {
    $pdo = new PDO("mysql:host=$host;dbname=$database;charset=utf8mb4", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) {
    $response['status'] = 'error';
    $response['message'] = 'Ошибка подключения к базе данных: ' . $e->getMessage();
    echo json_encode($response, JSON_UNESCAPED_UNICODE);
    exit;
}

// Получение JSON-данных
$json_data = null;
if (isset($_FILES['json_file']) && $_FILES['json_file']['error'] === UPLOAD_ERR_OK) {
    $json_tmp_name = $_FILES['json_file']['tmp_name'];
    $json_content = file_get_contents($json_tmp_name);
    $json_data = json_decode($json_content, true);
    if (json_last_error() !== JSON_ERROR_NONE) {
        $response['status'] = 'error';
        $response['message'] = 'Ошибка при декодировании JSON.';
        echo json_encode($response, JSON_UNESCAPED_UNICODE);
        exit;
    }
} else {
    $response['status'] = 'error';
    $response['message'] = 'JSON-файл не был загружен.';
    echo json_encode($response, JSON_UNESCAPED_UNICODE);
    exit;
}

if ($json_data) {
    // Обработка пользователей
    if (isset($json_data['users']) && is_array($json_data['users'])) {
        foreach ($json_data['users'] as $user) {
            if (isset($user['username']) && isset($user['password'])) {
                try {
                    // Проверяем, существует ли пользователь с таким именем
                    $stmt_check_user = $pdo->prepare("SELECT id FROM users WHERE username = :username");
                    $stmt_check_user->bindParam(':username', $user['username']);
                    $stmt_check_user->execute();
                    $existing_user_id = $stmt_check_user->fetchColumn();

                    if ($existing_user_id) {

                        $stmt_update_user = $pdo->prepare("UPDATE users SET password = :password WHERE id = :id");
                        $stmt_update_user->bindParam(':password', $user['password']);
                        $stmt_update_user->bindParam(':id', $existing_user_id, PDO::PARAM_INT);
                        $stmt_update_user->execute();
                        $response['updated_users']++; // Инкрементируем счетчик обновленных пользователей
                        error_log("Пользователь " . $user['username'] . " обновлен.");
                    } else {
                        // Пользователь не существует, вставляем нового
                        $stmt_insert_user = $pdo->prepare("INSERT INTO users (username, password) VALUES (:username, :password)");
                        $stmt_insert_user->bindParam(':username', $user['username']);
                        $stmt_insert_user->bindParam(':password', $user['password']);
                        $stmt_insert_user->execute();
                        $response['inserted']['users']++; // Инкрементируем счетчик вставленных пользователей
                        error_log("Пользователь " . $user['username'] . " добавлен.");
                    }
                } catch (PDOException $e) {
                    error_log("Ошибка обработки пользователя: " . $e->getMessage());
                    $response['status'] = 'warning';
                    $response['message'] .= ' Ошибка при обработке пользователя ' . $user['username'] . ': ' . $e->getMessage();
                }
            } else {
                $response['status'] = 'warning';
                $response['message'] .= ' Некорректные данные пользователя.';
            }
        }
    }
    if (isset($json_data['map_points']) && is_array($json_data['map_points'])) {
        $stmt = $pdo->prepare("
        INSERT INTO map_points (id, userId, label, description, latitude, longitude, photoUris)
        VALUES (:id, :userId, :label, :description, :latitude, :longitude, :photoUris)
        ON DUPLICATE KEY UPDATE
            userId = :userId,
            label = :label,
            description = :description,
            latitude = :latitude,
            longitude = :longitude,
            photoUris = :photoUris
         ");
        error_log("FILES: " . print_r($_FILES, true));
        foreach ($json_data['map_points'] as $point) {
            if (isset($point['id']) && isset($point['userId']) && isset($point['label']) && isset($point['latitude']) && isset($point['longitude'])) {
                $uploadedUris = [];
                if (isset($_FILES['images']) && is_array($_FILES['images']['name'])) {
                    foreach ($_FILES['images']['error'] as $key => $error) {
                        if ($error === UPLOAD_ERR_OK) {
                            $tmp_name = $_FILES['images']['tmp_name'][$key];
                            $name = basename($_FILES['images']['name'][$key]);
                            $destination = $uploadDirImages . $name; // Используем оригинальное имя

                            // Проверяем, существует ли файл с таким именем
                            if (!file_exists($destination)) {
                                if (move_uploaded_file($tmp_name, $destination)) {
                                    $uploadedUris[] = $destination;
                                    $response['uploaded_images'][] = $destination;
                                } else {
                                    $response['status'] = 'warning';
                                    $response['message'] .= ' Ошибка при загрузке изображения: ' . $name . '.';
                                }
                            } else {
                                $uploadedUris[] = $destination; // Файл уже существует, просто добавляем его путь
                                $response['uploaded_images'][] = $destination;
                            }
                        }
                    }
                }
                $existingUris = [];
                if (isset($point['photoUris'])) {
                    // Проверяем, является ли photoUris строкой, прежде чем декодировать
                    $existingUris = is_string($point['photoUris']) ? json_decode($point['photoUris'], true) ?? [] : $point['photoUris'];
                }
                $photoUrisForDb = json_encode(array_unique(array_merge($existingUris, $uploadedUris)));

                // Явно определяем значения и типы для привязки
                $descriptionValue = $point['description'] ?? null;
                $descriptionType = isset($point['description']) ? PDO::PARAM_STR : PDO::PARAM_NULL;

                // photoUrisForDb будет всегда строкой (JSON '[]' или '["..."]')
                $photoUrisValue = $photoUrisForDb;
                $photoUrisType = PDO::PARAM_STR;

                // Логирование значений перед привязкой
                error_log("Binding values for map_point: id=" . $point['id'] .
                    ", userId=" . $point['userId'] .
                    ", label=" . $point['label'] .
                    ", description=" . ($descriptionValue ?? 'NULL') .
                    ", latitude=" . $point['latitude'] .
                    ", longitude=" . $point['longitude'] .
                    ", photoUris=" . $photoUrisValue);

                // Объединяем существующие URI с новыми загруженными и удаляем дубликаты
                $photoUrisForDb = json_encode(array_unique(array_merge($existingUris, $uploadedUris)));
                try {
                    $stmt->bindParam(':id', $point['id']);
                    $stmt->bindParam(':userId', $point['userId']); // Исправлено: используем :userId
                    $stmt->bindParam(':label', $point['label']);
                    $stmt->bindParam(':description', $point['description']);
                    $stmt->bindParam(':latitude', $point['latitude']);
                    $stmt->bindParam(':longitude', $point['longitude']);
                    $stmt->bindParam(':photoUris', $photoUrisForDb);
                    $execute_result = $stmt->execute();

                    if ($execute_result) {
                        if ($pdo->lastInsertId() > 0) {
                            $response['inserted']['map_points']++;
                        } else {
                            $rowCount = $stmt->rowCount();
                            if ($rowCount > 0) {
                                $response['updated']++;
                            }
                        }
                    } else {
                        // Если execute() вернул false, но не выбросил исключение
                        $errorInfo = $stmt->errorInfo();
                        error_log("SQL Execute Error: " . $errorInfo[2]);
                        $response['status'] = 'error';
                        $response['message'] .= ' Ошибка SQL при обработке точек карты: ' . $errorInfo[2];
                    }
                } catch (PDOException $e) {
                    error_log("Ошибка вставки/обновления точки карты: " . $e->getMessage());
                    $response['status'] = 'error'; // Изменим на 'error' для явного указания ошибки
                    $response['message'] .= ' Ошибка при обработке некоторых точек карты: ' . $e->getMessage();
                }
            } else {
                $response['status'] = 'warning';
                $response['message'] .= ' Некорректные данные точки карты.';
            }
        }
        $response['message'] = 'Обработано ' . ($response['inserted']['map_points'] + $response['updated']) . ' точек карты (' . $response['inserted']['map_points'] . ' добавлено, ' . $response['updated'] . ' обновлено).';
    }

// ... (вывод JSON-ответа) ...
}

echo json_encode($response, JSON_UNESCAPED_UNICODE);

