<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!doctype html>
<html lang="ru">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <title>Главная — TextQuest</title>

    <c:url var="cssMain" value="/assets/css/main.css"/>
    <link rel="stylesheet" href="${cssMain}"/>

    <c:set var="PATH_HOME" value="/"/>
    <c:set var="PATH_LOGIN" value="/login"/>
    <c:set var="PATH_REGISTER" value="/register"/>
    <c:set var="PATH_QUEST" value="/quest"/>
    <c:set var="PATH_QUESTS" value="/quests"/>
    <c:set var="PATH_LOGOUT" value="/logout"/>
    <c:set var="PATH_CREATE" value="/create_quest"/>
    <c:set var="PATH_MY_QUESTS" value="/my/quests"/>
    <c:set var="PATH_LOADS" value="/loads"/>

    <c:set var="PARAM_NEW" value="new"/>
    <c:set var="PARAM_NEXT" value="next"/>
    <c:set var="PARAM_PURPOSE" value="purpose"/>
    <c:set var="PURPOSE_LOAD" value="load"/>

    <c:url var="homeUrl" value="${PATH_HOME}"/>
    <c:url var="loginUrl" value="${PATH_LOGIN}"/>
    <c:url var="registerUrl" value="${PATH_REGISTER}"/>
    <c:url var="questUrl" value="${PATH_QUEST}"/>
    <c:url var="questsUrl" value="${PATH_QUESTS}"/>
    <c:url var="logoutUrl" value="${PATH_LOGOUT}"/>

    <c:url var="createNewUrl" value="${PATH_CREATE}">
        <c:param name="${PARAM_NEW}" value="1"/>
    </c:url>

    <c:url var="loadsUrl" value="${PATH_LOADS}">
        <c:param name="${PARAM_NEXT}" value="${PATH_QUEST}"/>
        <c:param name="${PARAM_PURPOSE}" value="${PURPOSE_LOAD}"/>
    </c:url>
</head>
<body>

<c:set var="user" value="${sessionScope.user}"/>
<c:set var="isAuth" value="${not empty user}"/>
<c:set var="userName" value="${isAuth ? user.userName : ''}"/>
<c:set var="avatar" value="${empty userName ? '?' : fn:toUpperCase(fn:substring(userName,0,1))}"/>

<main class="container fade-in" role="main">
    <article class="card" aria-labelledby="pageTitle">
        <header class="card-header">
            <div class="logo" aria-hidden="true"></div>
            <h1 id="pageTitle">TextQuest — интерактивные истории</h1>
        </header>

        <section class="card-body">
            <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

            <section class="section" aria-labelledby="aboutTitle">
                <h2 id="aboutTitle" class="user-title">О проекте</h2>
                <p class="notice">
                    TextQuest — платформа интерактивных текстовых приключений. Создавайте свои квесты,
                    делитесь ими с друзьями и проходите истории других авторов. Ваши выборы влияют на сюжет.
                </p>
            </section>

            <section class="panel" aria-label="Пользователь">
                <c:choose>
                    <c:when test="${isAuth}">
                        <div class="user">
                            <div class="avatar" aria-hidden="true"><c:out value="${avatar}"/></div>
                            <div>
                                <p class="user-title">Вы вошли как: <c:out value="${userName}"/></p>
                            </div>
                        </div>
                        <div class="actions">
                            <button type="button" class="btn btn-ghost" title="Профиль (скоро)">Профиль</button>
                            <button type="button" class="btn" title="Друзья (скоро)">Друзья</button>
                            <button type="button" class="btn" title="Почта (скоро)">
                                <svg class="icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                                    <path d="M3 6h18a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1zm0 0 9 7 9-7"
                                          fill="none" stroke="currentColor" stroke-width="2"
                                          stroke-linecap="round" stroke-linejoin="round"/>
                                </svg>
                                Почта
                            </button>
                            <form method="post" action="${logoutUrl}" style="display:inline">
                                <button type="submit" class="btn btn-danger">Выйти</button>
                            </form>
                        </div>
                    </c:when>

                    <c:otherwise>
                        <div>
                            <p class="user-title">Гость</p>
                            <p class="user-sub">Вы не вошли в систему.</p>
                        </div>
                        <nav class="actions" aria-label="Авторизация">
                            <a class="btn btn-primary" href="${loginUrl}">Войти</a>
                            <a class="btn btn-ghost" href="${registerUrl}">Зарегистрироваться</a>
                        </nav>
                    </c:otherwise>
                </c:choose>
            </section>

            <section class="section" aria-labelledby="quickNavTitle">
                <h2 id="quickNavTitle" class="user-title">Быстрые действия</h2>
                <div class="linkrow" role="navigation" aria-label="Основная навигация">
                    <a class="link" href="${questUrl}" title="Начать главный квест">Главный квест — начать</a>
                    <a class="link" href="${questsUrl}">Каталог квестов</a>
                    <c:if test="${isAuth}">
                        <a class="link" href="<c:url value='${PATH_MY_QUESTS}'/>">Мои квесты</a>
                    </c:if>
                </div>
            </section>

            <c:if test="${isAuth}">
                <section class="section" aria-labelledby="continueTitle">
                    <h2 id="continueTitle" class="user-title">Продолжить прохождение</h2>
                    <p class="notice">У вас есть сохранения — вернитесь к приключениям с последней сцены.</p>
                    <div class="actions">
                        <a class="btn btn-primary" href="${loadsUrl}" title="Продолжить">Продолжить</a>
                    </div>
                </section>

                <section class="section" aria-labelledby="createTitle">
                    <h2 id="createTitle" class="user-title">Творчество автора</h2>
                    <p class="notice">Создавайте ветвящийся сюжет, главы и варианты ответа. Публикуйте и собирайте
                        отзывы.</p>
                    <div class="actions">
                        <a class="btn btn-primary" href="${createNewUrl}" title="Создать квест">Создать квест</a>
                    </div>
                </section>
            </c:if>

            <c:if test="${not isAuth}">
                <section class="section" aria-labelledby="guestCtaTitle">
                    <h2 id="guestCtaTitle" class="user-title">Начните приключение</h2>
                    <p class="notice">Зарегистрируйтесь, чтобы сохранять прогресс и создавать собственные истории.</p>
                </section>
            </c:if>

        </section>
    </article>
</main>
</body>
</html>