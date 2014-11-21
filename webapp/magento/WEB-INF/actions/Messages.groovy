
eventNotificationMessageList = [];
if (session.getAttribute("_EVENT_MESSAGE_")) {
    eventMessageList.add(session.getAttribute("_EVENT_MESSAGE_"))
    session.removeAttribute("_EVENT_MESSAGE_");
}
context.eventNotificationMessageList = eventMessageList;
context.eventMessageList = [];
if (eventNotificationMessageList) {
    request.removeAttribute("eventMessageList");
    request.removeAttribute("_EVENT_MESSAGE_");
    request.removeAttribute("_EVENT_MESSAGE_LIST_");
}

errorNotificationMessageList = [];
if (session.getAttribute("_ERROR_MESSAGE_")) {
    errorNotificationMessageList.add(session.getAttribute("_ERROR_MESSAGE_"))
    session.removeAttribute("_ERROR_MESSAGE_");
}
context.errorNotificationMessageList = errorMessageList;
context.errorMessageList = [];
if (errorNotificationMessageList) {
    request.removeAttribute("errorMessageList");
    request.removeAttribute("_ERROR_MESSAGE_");
    request.removeAttribute("_ERROR_MESSAGE_LIST");
}
