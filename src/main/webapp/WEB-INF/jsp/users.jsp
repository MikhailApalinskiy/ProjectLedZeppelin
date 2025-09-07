<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8"/>
    <title>Пользователи — TextQuest</title>

    <c:url var="cssMain" value="/assets/css/main.css"/>
    <c:url var="cssUsers" value="/assets/css/users.css"/>
    <link rel="stylesheet" href="${cssMain}"/>
    <link rel="stylesheet" href="${cssUsers}"/>

    <meta name="viewport" content="width=device-width, initial-scale=1"/>
</head>
<body>
<div class="container">

    <c:set var="CTX" value="${pageContext.request.contextPath}"/>
    <c:set var="PATH_HOME" value="/"/>
    <c:set var="PATH_USERS" value="/users"/>
    <c:set var="PATH_EDIT_USER" value="/user/edit"/>

    <c:url var="homeUrl" value="${PATH_HOME}"/>
    <c:url var="usersUrl" value="${PATH_USERS}"/>

    <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

    <header class="card header">
        <div class="header__text">
            <h1 class="header__title">Пользователи</h1>
            <p class="muted header__sub">Список всех зарегистрированных пользователей.</p>
        </div>
        <nav class="actions">
            <button type="button" class="btn btn-primary" onclick="location.href='${homeUrl}'">
                На главную
            </button>
        </nav>
    </header>

    <section class="card">
        <form method="get" action="${usersUrl}" class="search-form" role="search" aria-label="Поиск пользователей">
            <input type="text"
                   name="q"
                   value="${param.q}"
                   placeholder="Поиск по нику или ID"
                   class="input search-input"
                   autocomplete="off"/>
            <div class="actions">
                <button type="submit" class="btn btn-primary">Искать</button>
                <c:if test="${not empty param.q}">
                    <a class="btn btn-ghost" href="${usersUrl}">Сбросить</a>
                </c:if>
            </div>
        </form>
    </section>

    <section class="card">
        <c:set var="isAuth" value="${not empty sessionScope.user}"/>
        <c:set var="isAdmin" value="${isAuth and sessionScope.user.role eq 'ADMIN'}"/>

        <c:choose>
            <c:when test="${empty users}">
                <p class="muted">Пользователи не найдены.</p>
            </c:when>
            <c:otherwise>
                <table class="list users-table" aria-label="Список пользователей">
                    <thead>
                    <tr>
                        <th class="col-num">#</th>
                        <th>Имя</th>
                        <th>Логин</th>
                        <th>ID</th>
                        <th class="col-role">Роль</th>
                        <th class="col-date">Создан</th>
                        <th class="col-actions">Действия</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="u" items="${users}" varStatus="st">
                        <c:set var="isSelf" value="${isAuth and sessionScope.user.userId eq u.userId}"/>
                        <c:set var="canAddFriend" value="${isAuth and not isSelf}"/>

                        <tr>
                            <td><span class="pill">${st.index + 1}</span></td>
                            <td><c:out value="${u.userName}"/></td>
                            <td><code><c:out value="${u.userLogin}"/></code></td>
                            <td><code><c:out value="${u.userId}"/></code></td>
                            <td class="role"><c:out value="${u.role}"/></td>
                            <td><fmt:formatDate value="${u.createdAtDate}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
                            <td class="col-actions td-actions">
                                <div class="actions actions--row">
                                    <c:if test="${canAddFriend}">
                                        <form method="post" action="${pageContext.request.contextPath}/friends"
                                              style="display:inline">
                                            <input type="hidden" name="action" value="request"/>
                                            <input type="hidden" name="id" value="${u.userId}"/>
                                            <button type="submit" class="btn">Добавить в друзья</button>
                                        </form>
                                    </c:if>
                                    <c:if test="${isAdmin and not isSelf}">
                                        <c:url var="editUrl" value="${PATH_EDIT_USER}">
                                            <c:param name="id" value="${u.userId}"/>
                                        </c:url>
                                        <a class="btn btn-ghost" href="${editUrl}">Редактировать</a>
                                    </c:if>
                                    <c:if test="${not canAddFriend and not (isAdmin and not isSelf)}">
                                        <span class="muted">—</span>
                                    </c:if>
                                </div>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </section>

</div>
</body>
</html>