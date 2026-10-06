<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="警報一覧" />
<c:set var="autoRefresh" value="${true}" />
<%@ include file="../common/header.jspf" %>
<h1>警報一覧 <small>60 秒ごとに自動更新</small></h1>
<div class="panel">
<table class="grid" id="alarms">
  <thead><tr><th>#</th><th>発生日時</th><th>変電所</th><th>遮断器</th><th>レベル</th><th>コード</th><th>内容</th><th>確認</th></tr></thead>
  <tbody>
  <c:forEach var="a" items="${alarms.content}">
    <tr class="${a.acknowledged ? '' : 'unacked'}">
      <td class="num">${a.id}</td>
      <td><fmt:formatDate value="${a.occurredAt}" pattern="yyyy/MM/dd HH:mm" /></td>
      <td><a href="${ctx}/power/substations/${a.substationCode}">${a.substationCode}</a></td>
      <td><c:out value="${a.breakerCode}" /></td>
      <td><span class="lv-${a.level}">${a.level.label}</span></td>
      <td>${a.alarmCode}</td>
      <td><c:out value="${a.message}" /></td>
      <td>
        <c:choose>
          <c:when test="${a.acknowledged}"><c:out value="${a.ackBy}" /> <fmt:formatDate value="${a.ackAt}" pattern="HH:mm" /></c:when>
          <c:otherwise>
            <sec:authorize access="hasAnyRole('DISPATCHER','ADMIN')">
            <form action="${ctx}/power/alarms/${a.id}/ack" method="post" class="inline">
              <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
              <button type="submit" class="primary">確認</button>
            </form>
            </sec:authorize>
            <sec:authorize access="!hasAnyRole('DISPATCHER','ADMIN')">未確認</sec:authorize>
          </c:otherwise>
        </c:choose>
      </td>
    </tr>
  </c:forEach>
  <c:if test="${empty alarms.content}"><tr><td colspan="8" class="empty">警報なし</td></tr></c:if>
  </tbody>
</table>
<div class="pager">
  <c:if test="${alarms.hasPrevious()}"><a href="?page=${alarms.number - 1}">&laquo; 前へ</a></c:if>
  ${alarms.number + 1} / ${alarms.totalPages}
  <c:if test="${alarms.hasNext()}"><a href="?page=${alarms.number + 1}">次へ &raquo;</a></c:if>
</div>
</div>
<%@ include file="../common/footer.jspf" %>
