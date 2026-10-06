<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="${outage.requestNo}" />
<%@ include file="../common/header.jspf" %>
<h1>停電作業 ${outage.requestNo} <small class="st-${outage.status}">${outage.status.label}</small></h1>
<div class="panel">
  <table class="kv" id="outage">
    <tr><th>申請番号</th><td>${outage.requestNo}</td></tr>
    <tr><th>変電所</th><td>${outage.breaker.substation.code} <c:out value="${outage.breaker.substation.name}" /></td></tr>
    <tr><th>遮断器</th><td>${outage.breaker.breakerCode} <c:out value="${outage.breaker.name}" /> (現在: <span class="bk-${outage.breaker.state}">${outage.breaker.state.label}</span>)</td></tr>
    <tr><th>作業日時</th><td><fmt:formatDate value="${outage.workDate}" pattern="yyyy/MM/dd" /> ${outage.startTime} 〜 ${outage.endTime}</td></tr>
    <tr><th>作業内容</th><td><c:out value="${outage.description}" /></td></tr>
    <tr><th>状態</th><td class="st-${outage.status}" id="outage-status">${outage.status.label}</td></tr>
    <tr><th>申請</th><td><c:out value="${outage.requestedBy}" /> <fmt:formatDate value="${outage.requestedAt}" pattern="yyyy/MM/dd HH:mm" /></td></tr>
    <tr><th>承認</th><td><c:out value="${outage.approvedBy}" /> <fmt:formatDate value="${outage.approvedAt}" pattern="yyyy/MM/dd HH:mm" /></td></tr>
    <tr><th>備考</th><td><c:out value="${outage.remarks}" /></td></tr>
  </table>
  <sec:authorize access="hasAnyRole('DISPATCHER','ADMIN')">
  <p>
  <c:if test="${outage.status == 'REQUESTED'}">
    <form action="${ctx}/power/outages/${outage.requestNo}/approve" method="post" class="inline js-confirm" data-confirm="承認しますか？">
      <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" /><button type="submit" class="primary">承認</button></form>
    <form action="${ctx}/power/outages/${outage.requestNo}/reject" method="post" class="inline">
      <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
      <input type="text" name="remarks" placeholder="却下理由" size="30"><button type="submit">却下</button></form>
  </c:if>
  <c:if test="${outage.status == 'APPROVED'}">
    <form action="${ctx}/power/outages/${outage.requestNo}/start" method="post" class="inline js-confirm" data-confirm="${outage.breaker.breakerCode} を開放 (き電停止) します。よろしいですか？">
      <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" /><button type="submit" class="warn">き電停止</button></form>
  </c:if>
  <c:if test="${outage.status == 'IN_PROGRESS'}">
    <form action="${ctx}/power/outages/${outage.requestNo}/complete" method="post" class="inline js-confirm" data-confirm="作業完了を確認し ${outage.breaker.breakerCode} を投入 (復電) します。よろしいですか？">
      <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" /><button type="submit" class="primary">復電</button></form>
  </c:if>
  </p>
  </sec:authorize>
  <p><a href="${ctx}/power/outages">一覧へ戻る</a></p>
</div>
<%@ include file="../common/footer.jspf" %>
