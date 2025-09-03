<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <title>Слоты сохранений — TextQuest</title>

    <c:url var="cssMain"  value="/assets/css/main.css"/>
    <c:url var="cssSaves" value="/assets/css/saves.css"/>
    <link rel="stylesheet" href="${cssMain}">
    <link rel="stylesheet" href="${cssSaves}">
</head>
<body>

<c:url var="homeUrl"      value="/"/>
<c:url var="savesAction"  value="/saves"/>

<main class="container fade-in" role="main">
    <article class="card" aria-labelledby="title">
        <header class="card-header" style="justify-content: space-between;">
            <h1 id="title">Слоты сохранений</h1>
            <a href="${homeUrl}" class="btn btn-ghost" id="goHome">На главную</a>
        </header>

        <section class="card-body">
            <%@ include file="/WEB-INF/jsp/fragments/alerts.jspf" %>

            <c:url var="continueUrl" value="${empty param.next ? '/quest' : param.next}">
                <c:param name="id" value="${param.node}"/>
            </c:url>

            <c:if test="${not empty param.node}">
                <div class="actions" style="justify-content:flex-end; margin-bottom:10px">
                    <a class="btn" href="${continueUrl}">Продолжить</a>
                </div>
            </c:if>

            <p class="notice">Выберите слот, чтобы продолжить, или создайте новое сохранение.</p>

            <c:if test="${not empty param.node}">
                <p class="notice">Будет сохранён узел #<c:out value="${param.node}"/>.</p>
            </c:if>

            <div class="slots">
                <c:choose>
                    <c:when test="${not empty slots}">
                        <c:forEach var="slot" items="${slots}" varStatus="st">
                            <c:set var="idx"     value="${slot.index}"/>
                            <c:set var="nodeId"  value="${slot.nodeId}"/>
                            <c:set var="isEmpty" value="${nodeId == null or nodeId lt 0}"/>
                            <c:set var="questId" value="${empty slot.questId ? 'main' : slot.questId}"/>
                            <c:set var="title"   value="${slot.title}"/>
                            <c:set var="updated" value="${slot.updatedAtText}"/>

                            <div class="slot-row">
                                <form method="post" action="${savesAction}" style="margin:0">
                                    <input type="hidden" name="op"      value="go"/>
                                    <input type="hidden" name="slot"    value="${idx}"/>
                                    <input type="hidden" name="next"    value="${param.next}"/>
                                    <input type="hidden" name="purpose" value="${param.purpose}"/>
                                    <input type="hidden" name="node"    value="${param.node}"/>

                                    <button type="submit"
                                            class="slot-btn"
                                            id="slot-${idx}"
                                            data-slot="${idx}"
                                            data-node-id="<c:out value='${isEmpty ? "" : nodeId}'/>"
                                            data-quest-id="<c:out value='${questId}'/>">
                                        <div class="slot-label">
                                            Слот ${idx + 1} —
                                            <c:choose>
                                                <c:when test="${isEmpty}">
                                                    Новое сохранение
                                                </c:when>
                                                <c:otherwise>
                                                    <c:out value="${not empty title ? title : ('Узел #' + nodeId)}"/>
                                                    <c:if test="${not empty updated}">
                                                        &nbsp;•&nbsp;<span class="slot-updated"><c:out value="${updated}"/></span>
                                                    </c:if>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                    </button>
                                </form>

                                <form method="post" action="${savesAction}" style="margin:0">
                                    <input type="hidden" name="op"      value="delete"/>
                                    <input type="hidden" name="slot"    value="${idx}"/>
                                    <input type="hidden" name="next"    value="${param.next}"/>
                                    <input type="hidden" name="purpose" value="${param.purpose}"/>
                                    <input type="hidden" name="node"    value="${param.node}"/>

                                    <button type="submit" class="btn btn-danger slot-delete" id="del-${idx}">
                                        Удалить
                                    </button>
                                </form>
                            </div>
                        </c:forEach>
                    </c:when>

                    <c:otherwise>
                        <c:forEach var="i" begin="0" end="9">
                            <div class="slot-row">
                                <form method="post" action="${savesAction}" style="margin:0">
                                    <input type="hidden" name="op"      value="go"/>
                                    <input type="hidden" name="slot"    value="${i}"/>
                                    <input type="hidden" name="next"    value="${param.next}"/>
                                    <input type="hidden" name="purpose" value="${param.purpose}"/>
                                    <input type="hidden" name="node"    value="${param.node}"/>

                                    <button type="submit"
                                            class="slot-btn"
                                            id="slot-${i}"
                                            data-slot="${i}"
                                            data-node-id=""
                                            data-quest-id="main">
                                        <div class="slot-label">
                                            Слот ${i + 1} — Новое сохранение
                                        </div>
                                    </button>
                                </form>

                                <form method="post" action="${savesAction}" style="margin:0">
                                    <input type="hidden" name="op"      value="delete"/>
                                    <input type="hidden" name="slot"    value="${i}"/>
                                    <input type="hidden" name="next"    value="${param.next}"/>
                                    <input type="hidden" name="purpose" value="${param.purpose}"/>
                                    <input type="hidden" name="node"    value="${param.node}"/>

                                    <button type="submit" class="btn btn-danger slot-delete" id="del-${i}">
                                        Удалить
                                    </button>
                                </form>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>
        </section>
    </article>
</main>

</body>
</html>