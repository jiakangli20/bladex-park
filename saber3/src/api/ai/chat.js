import request from '@/axios';
import { Base64 } from 'js-base64';
import website from '@/config/website';
import { baseUrl } from '@/config/env';
import { getToken } from '@/utils/auth';

export const listConversations = () =>
  request({
    url: '/blade-ai/chat/conversations',
    method: 'get',
  });

export const listMessages = conversationId =>
  request({
    url: '/blade-ai/chat/messages',
    method: 'get',
    params: { conversationId },
  });

export const sendMessage = data =>
  request({
    url: '/blade-ai/chat/send',
    method: 'post',
    data,
  });

export const deleteConversation = conversationId =>
  request({
    url: '/blade-ai/chat/conversations/remove',
    method: 'post',
    params: { conversationId },
  });

/**
 * 以 SSE 读取 AI 增量回复。普通 axios 响应拦截器会等待完整 body，因此流式接口使用 fetch。
 */
export const streamMessage = async (data, onEvent) => {
  const response = await fetch(`${baseUrl || ''}/blade-ai/chat/send/stream`, {
    method: 'POST',
    credentials: 'include',
    headers: {
      Authorization: `Basic ${Base64.encode(`${website.clientId}:${website.clientSecret}`)}`,
      [website.tokenHeader]: `bearer ${getToken() || ''}`,
      'Blade-Requested-With': 'BladeHttpRequest',
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
    },
    body: JSON.stringify(data),
  });
  if (!response.ok || !response.body) {
    throw new Error(`AI 请求失败（${response.status}）`);
  }
  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';
  const consume = raw => {
    buffer += raw;
    const events = buffer.split(/\r?\n\r?\n/);
    buffer = events.pop() || '';
    events.forEach(eventText => {
      let eventName = 'message';
      let payload = '';
      eventText.split(/\r?\n/).forEach(line => {
        if (line.startsWith('event:')) eventName = line.slice(6).trim();
        if (line.startsWith('data:')) payload += line.slice(5).trim();
      });
      if (payload) onEvent(eventName, JSON.parse(payload));
    });
  };
  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    consume(decoder.decode(value, { stream: true }));
  }
  consume(decoder.decode());
};
