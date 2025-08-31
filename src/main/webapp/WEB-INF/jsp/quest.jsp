<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<link rel="stylesheet" href="<c:url value='/assets/css/main.css'/>"/>

<style>
    .quest-image {
        max-width: 100%;
        height: auto;
        display: block;
        border-radius: 12px;
        margin: 0 auto;
        box-shadow: var(--shadow);
    }

    .answers {
        display: flex;
        flex-wrap: wrap;
        gap: 10px;
        justify-content: center;
    }
</style>

<c:choose>
    <c:when test="${node == null}">
        <c:set var="pageTitle" value="TextQuest — Ошибка"/>
    </c:when>
    <c:when test="${node.fin}">
        <c:set var="pageTitle" value="TextQuest — Финал #${node.id}"/>
    </c:when>
    <c:otherwise>
        <c:set var="pageTitle" value="TextQuest — Ветка #${node.id}"/>
    </c:otherwise>
</c:choose>

<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8"/>
    <title><c:out value="${pageTitle}"/></title>
</head>
<body>

<c:url value="/quest" var="questAction"/>
<c:url value="/assets/img/placeholder.jpg" var="placeholder"/>

<div class="container">
    <div class="card fade-in">
        <div class="card-header">
            <div class="logo"></div>
            <h1>Текстовый квест</h1>
        </div>

        <div class="card-body">
            <c:if test="${not empty error}">
                <div class="alert alert-error">
                    <div class="alert-dot"></div>
                    <div class="alert-text"><c:out value="${error}"/></div>
                </div>
            </c:if>

            <c:choose>
                <c:when test="${node == null}">
                    <div class="stack" style="text-align:center">
                        <h2>Узел не найден</h2>
                        <p class="notice">Попробуйте начать заново.</p>
                        <a class="btn btn-primary" href="${questAction}">Начать с начала</a>
                    </div>
                </c:when>

                <c:otherwise>
                    <div class="stack">

                        <c:choose>
                            <c:when test="${not empty node.image}">
                                <c:set var="rawImg" value="${node.image}"/>
                                <c:choose>
                                    <c:when test="${fn:startsWith(rawImg, '/')}">
                                        <c:url value="${rawImg}" var="imgSrc"/>
                                    </c:when>
                                    <c:otherwise>
                                        <c:url value="/${rawImg}" var="imgSrc"/>
                                    </c:otherwise>
                                </c:choose>
                            </c:when>
                            <c:otherwise>
                                <c:set var="imgSrc" value="${placeholder}"/>
                            </c:otherwise>
                        </c:choose>

                        <img class="quest-image"
                             src="${imgSrc}"
                             alt="Сцена узла #${node.id}"
                             loading="lazy"
                             onerror="this.onerror=null; this.src='${placeholder}'"/>

                        <div class="panel">
                            <div class="max-ch">
                                <h2>Ветка #<c:out value="${node.id}"/></h2>
                                <p><c:out value="${node.text}"/></p>
                            </div>
                        </div>

                        <c:choose>
                            <c:when test="${node.fin}">
                                <div class="actions" style="justify-content:center">
                                    <a class="btn btn-primary" href="${questAction}">Начать с начала</a>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <form class="form stack" method="post" action="${questAction}">
                                    <input type="hidden" name="fromId" value="${node.id}"/>
                                    <div class="answers">
                                        <c:forEach items="${node.options}" var="opt">
                                            <button class="btn" type="submit" name="answer" value="${opt.choice}">
                                                <c:out value="${opt.choice}"/>
                                            </button>
                                        </c:forEach>
                                    </div>
                                </form>
                            </c:otherwise>
                        </c:choose>

                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

</body>
</html>