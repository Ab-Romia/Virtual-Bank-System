import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { assistant } from '../api/endpoints';
import { errorMessage } from '../api/client';
import { Button, Card, ErrorNote, Input } from '../components/ui';

interface Message {
  role: 'you' | 'assistant';
  text: string;
}

export function AssistantPage() {
  const [input, setInput] = useState('');
  const [messages, setMessages] = useState<Message[]>([]);

  const ask = useMutation({
    mutationFn: (message: string) => assistant.chat(message),
    onSuccess: (data) =>
      setMessages((prev) => [...prev, { role: 'assistant', text: data.reply }]),
  });

  const onSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    const message = input.trim();
    if (!message) return;
    setMessages((prev) => [...prev, { role: 'you', text: message }]);
    setInput('');
    ask.mutate(message);
  };

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div>
        <h1 className="text-xl font-semibold tracking-tight">Assistant</h1>
        <p className="mt-1 text-sm text-muted">
          Ask about accounts, transfers, or bank policies.
        </p>
      </div>

      <Card className="flex h-[28rem] flex-col">
        <div className="flex-1 space-y-3 overflow-y-auto p-5">
          {messages.length === 0 && !ask.isPending && (
            <p className="text-sm text-muted">No messages yet. Ask a question to begin.</p>
          )}
          {messages.map((message, index) => (
            <div
              key={index}
              className={
                message.role === 'you' ? 'flex justify-end' : 'flex justify-start'
              }
            >
              <div
                className={
                  message.role === 'you'
                    ? 'max-w-[80%] rounded-lg bg-accent px-3 py-2 text-sm text-white'
                    : 'max-w-[80%] rounded-lg bg-paper px-3 py-2 text-sm text-ink'
                }
              >
                {message.text}
              </div>
            </div>
          ))}
          {ask.isPending && (
            <div className="flex justify-start">
              <div className="rounded-lg bg-paper px-3 py-2 text-sm text-muted">
                Thinking...
              </div>
            </div>
          )}
        </div>

        <form onSubmit={onSubmit} className="border-t border-line p-3">
          {ask.isError && (
            <div className="mb-2">
              <ErrorNote>
                {errorMessage(ask.error, 'The assistant is unavailable right now')}
              </ErrorNote>
            </div>
          )}
          <div className="flex gap-2">
            <Input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Type a message"
              aria-label="Message"
            />
            <Button type="submit" loading={ask.isPending} disabled={!input.trim()}>
              Send
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}
