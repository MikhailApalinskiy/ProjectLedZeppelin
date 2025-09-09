<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>Редактор пользователя — TextQuest</title>

    <c:url var="cssMain" value="/assets/css/main.css"/>
    <c:url var="cssEdit" value="/assets/css/user-edit.css"/>
    <link rel="stylesheet" href="${cssMain}"/>
    <link rel="stylesheet" href="${cssEdit}"/>
</head>
<body class="page-user-edit">
<div class="container grid">

    <c:set var="PATH_HOME" value="/"/>
    <c:set var="PATH_USERS" value="/users"/>
    <c:set var="PATH_USER_EDIT" value="/user/edit"/>

    <c:set var="PARAM_ID" value="id"/>
    <c:set var="PARAM_ROLE" value="role"/>
    <c:set var="PARAM_NAME" value="userName"/>
    <c:set var="PARAM_LOGIN" value="userLogin"/>
    <c:set var="PARAM_PASS" value="password"/>

    <c:url var="homeUrl" value="${PATH_HOME}"/>
    <c:url var="usersUrl" value="${PATH_USERS}"/>
    <c:url var="editUrl" value="${PATH_USER_EDIT}">
        <c:param name="${PARAM_ID}" value="${param.id}"/>
    </c:url>

    <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

    <header class="card page-header">
        <div class="page-header__text">
            <h1 class="page-header__title">Редактор пользователя</h1>
            <p class="page-header__sub muted">
                ID: <code class="code"><c:out value="${editUser.userId}"/></code>
                · Создан: <fmt:formatDate value="${editUser.createdAtDate}" pattern="yyyy-MM-dd HH:mm:ss"/>
            </p>
        </div>
        <nav class="actions">
            <a class="btn btn-ghost" href="${usersUrl}">Список пользователей</a>
            <a class="btn btn-primary" href="${homeUrl}">На главную</a>
        </nav>
    </header>

    <section class="card">
        <form method="post" action="${editUrl}" class="form" autocomplete="off" novalidate aria-labelledby="formTitle">
            <h2 id="formTitle" class="sr-only">Форма редактирования пользователя</h2>
            <input type="hidden" name="${PARAM_ID}" value="${editUser.userId}"/>

            <div class="field">
                <label class="label" for="role">Роль</label>
                <select id="role" name="${PARAM_ROLE}" class="input select" required>
                    <option value="USER"  ${editUser.role eq 'USER'  ? 'selected' : ''}>USER</option>
                    <option value="ADMIN" ${editUser.role eq 'ADMIN' ? 'selected' : ''}>ADMIN</option>
                </select>
                <p class="help">Права администратора дают доступ к модерации и настройкам.</p>
            </div>

            <div class="field">
                <label class="label" for="userName">Отображаемое имя</label>
                <input id="userName" name="${PARAM_NAME}" class="input"
                       type="text" minlength="2" maxlength="60" required
                       autocomplete="name"
                       value="${editUser.userName}" placeholder="Имя пользователя"/>
            </div>

            <div class="field">
                <label class="label" for="userLogin">Логин</label>
                <input id="userLogin" name="${PARAM_LOGIN}" class="input"
                       type="text" minlength="3" maxlength="60" required
                       autocomplete="username"
                       value="${editUser.userLogin}" placeholder="Уникальный логин"/>
                <p class="help">При смене логина проверяется уникальность среди всех пользователей.</p>
            </div>

            <div class="field">
                <label class="label" for="password">Новый пароль (опционально)</label>
                <input id="password" name="${PARAM_PASS}" class="input" type="password" minlength="6"
                       autocomplete="new-password"
                       placeholder="Оставьте пустым, чтобы не менять"/>
                <p class="help">Пароль менять необязательно.</p>
            </div>

            <div class="actions actions--form">
                <button class="btn btn-primary" type="submit">Сохранить</button>
                <button class="btn btn-ghost" type="reset">Сбросить</button>
            </div>
        </form>
    </section>

    <section class="card">
        <h3 class="section-title">Информация</h3>
        <ul class="info">
            <li>При смене логина проверяется уникальность среди всех пользователей.</li>
            <li>Пароль менять необязательно — оставьте поле пустым.</li>
            <li>Если админ редактирует себя — данные в сессии обновятся автоматически.</li>
        </ul>
    </section>

</div>
</body>
</html>