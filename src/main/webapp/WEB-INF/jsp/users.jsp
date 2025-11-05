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
    <c:set var="PATH_FRIENDS" value="/friends"/>
    <c:set var="PARAM_Q" value="q"/>
    <c:set var="PARAM_ID" value="id"/>
    <c:set var="PARAM_ACTION" value="action"/>
    <c:set var="ACTION_REQUEST" value="request"/>
    <c:set var="ROLE_ADMIN" value="ADMIN"/>
    <c:set var="DATE_PATTERN" value="yyyy-MM-dd HH:mm:ss"/>

    <c:url var="homeUrl" value="${PATH_HOME}"/>
    <c:url var="usersUrl" value="${PATH_USERS}"/>

    <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

    <header class="card header">
        <div class="header__text">
            <h1 class="header__title">Пользователи</h1>
            <p class="muted header__sub">Список всех зарегистрированных пользователей.</p>
        </div>
        <nav class="actions">
            <button type="button" class="btn btn-primary btn-lg" onclick="location.href='${homeUrl}'">
                На главную
            </button>
        </nav>
    </header>

    <section class="card">
        <form method="get" action="${usersUrl}" class="search-form" role="search" aria-label="Поиск пользователей">
            <input type="text"
                   name="${PARAM_Q}"
                   value="${param[PARAM_Q]}"
                   placeholder="Поиск по логину или ID"
                   class="input search-input"
                   autocomplete="off"/>
            <div class="actions">
                <button type="submit" class="btn btn-primary">Искать</button>
                <c:if test="${not empty param[PARAM_Q]}">
                    <a class="btn btn-ghost" href="${usersUrl}">Сбросить</a>
                </c:if>
            </div>
        </form>
    </section>

    <section class="card">
        <c:set var="cur" value="${empty requestScope.user ? sessionScope.user : requestScope.user}"/>
        <c:set var="isAuth" value="${not empty cur}"/>
        <c:set var="isAdmin" value="${isAuth and cur.role eq ROLE_ADMIN}"/>

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
                        <c:set var="isSelf" value="${isAuth and cur.userId eq u.userId}"/>
                        <c:set var="canAddFriend" value="${isAuth and not isSelf}"/>

                        <tr>
                            <td><span class="pill">${offset + st.index + 1}</span></td>
                            <td><c:out value="${u.userName}"/></td>
                            <td><code><c:out value="${u.userLogin}"/></code></td>
                            <td><code><c:out value="${u.userId}"/></code></td>
                            <td class="role"><c:out value="${u.role}"/></td>
                            <td><fmt:formatDate value="${u.createdAtDate}" pattern="${DATE_PATTERN}"/></td>
                            <td class="col-actions td-actions">
                                <div class="actions actions--row">
                                    <c:if test="${canAddFriend}">
                                        <form method="post" action="${CTX}${PATH_FRIENDS}" style="display:inline">
                                            <input type="hidden" name="${PARAM_ACTION}" value="${ACTION_REQUEST}"/>
                                            <input type="hidden" name="${PARAM_ID}" value="${u.userId}"/>
                                            <button type="submit" class="btn">Добавить в друзья</button>
                                        </form>
                                    </c:if>

                                    <c:if test="${isAdmin and not isSelf}">
                                        <c:url var="editUrl" value="${PATH_EDIT_USER}">
                                            <c:param name="${PARAM_ID}" value="${u.userId}"/>
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

                <c:if test="${pages > 1}">
                    <nav class="pagination" aria-label="Навигация по страницам"
                         style="margin-top: 16px; display:flex; gap:6px; flex-wrap:wrap; justify-content:center;">
                        <c:set var="baseUrl" value="${pageContext.request.contextPath}/users"/>
                        <c:set var="qParam" value="${empty param.q ? '' : '&q=' += param.q}"/>

                        <c:choose>
                            <c:when test="${page > 1}">
                                <a class="btn btn-ghost" href="${baseUrl}?page=${page - 1}${qParam}">« Назад</a>
                            </c:when>
                            <c:otherwise>
                                <span class="btn btn-ghost muted" aria-disabled="true">« Назад</span>
                            </c:otherwise>
                        </c:choose>

                        <c:forEach var="p" begin="1" end="${pages}">
                            <c:choose>
                                <c:when test="${p == page}">
                                    <span class="btn btn-primary" aria-current="page">${p}</span>
                                </c:when>
                                <c:otherwise>
                                    <a class="btn" href="${baseUrl}?page=${p}${qParam}">${p}</a>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>

                        <c:choose>
                            <c:when test="${page < pages}">
                                <a class="btn btn-ghost" href="${baseUrl}?page=${page + 1}${qParam}">Вперёд »</a>
                            </c:when>
                            <c:otherwise>
                                <span class="btn btn-ghost muted" aria-disabled="true">Вперёд »</span>
                            </c:otherwise>
                        </c:choose>
                    </nav>
                </c:if>
            </c:otherwise>
        </c:choose>
    </section>

</div>
</body>
</html>