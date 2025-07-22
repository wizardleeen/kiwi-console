You are an expert AI frontend developer.

Your task is to modify an existing React application based on the provided files and a change description.

### Change Description

{}

### Current Source Files

{}

### Updated Backend API (`src/api.ts`)

```typescript
{}
```

### Dependencies

You are only allowed to use libraries declared in the following package.json and you are not allowed to modify it:

```json
{
  "name": "kiwi-pages",
  "private": true,
  "version": "0.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc -b && vite build",
    "lint": "eslint .",
    "preview": "vite preview"
  },
  "dependencies": {
    "@hookform/resolvers": "^5.1.1",
    "@radix-ui/react-accordion": "^1.2.11",
    "@radix-ui/react-alert-dialog": "^1.1.14",
    "@radix-ui/react-aspect-ratio": "^1.1.7",
    "@radix-ui/react-avatar": "^1.1.10",
    "@radix-ui/react-checkbox": "^1.3.2",
    "@radix-ui/react-collapsible": "^1.1.11",
    "@radix-ui/react-context-menu": "^2.2.15",
    "@radix-ui/react-dialog": "^1.1.14",
    "@radix-ui/react-dropdown-menu": "^2.1.15",
    "@radix-ui/react-hover-card": "^1.1.14",
    "@radix-ui/react-label": "^2.1.7",
    "@radix-ui/react-menubar": "^1.1.15",
    "@radix-ui/react-navigation-menu": "^1.2.13",
    "@radix-ui/react-popover": "^1.1.14",
    "@radix-ui/react-progress": "^1.1.7",
    "@radix-ui/react-radio-group": "^1.3.7",
    "@radix-ui/react-scroll-area": "^1.2.9",
    "@radix-ui/react-select": "^2.2.5",
    "@radix-ui/react-separator": "^1.1.7",
    "@radix-ui/react-slider": "^1.3.5",
    "@radix-ui/react-slot": "^1.2.3",
    "@radix-ui/react-switch": "^1.2.5",
    "@radix-ui/react-tabs": "^1.1.12",
    "@radix-ui/react-toggle": "^1.1.9",
    "@radix-ui/react-toggle-group": "^1.1.10",
    "@radix-ui/react-tooltip": "^1.2.7",
    "@tanstack/react-table": "^8.21.3",
    "@types/lodash": "^4.17.20",
    "ahooks": "^3.9.0",
    "class-variance-authority": "^0.7.1",
    "clsx": "^2.1.1",
    "cmdk": "^1.1.1",
    "date-fns": "^4.1.0",
    "dayjs": "^1.11.13",
    "embla-carousel-react": "^8.6.0",
    "input-otp": "^1.4.2",
    "lodash": "^4.17.21",
    "lucide-react": "^0.525.0",
    "next-themes": "^0.4.6",
    "react": "^19.1.0",
    "react-day-picker": "^9.8.0",
    "react-dom": "^19.1.0",
    "react-hook-form": "^7.60.0",
    "react-resizable-panels": "^3.0.3",
    "react-router": "^6",
    "react-router-dom": "^6",
    "recharts": "2.15.4",
    "sonner": "^2.0.6",
    "tailwind-merge": "^3.3.1",
    "tailwindcss": "^4.1.11",
    "vaul": "^1.1.2"
  },
  "devDependencies": {
    "@eslint/js": "^9.30.1",
    "@tailwindcss/vite": "^4.1.11",
    "@types/node": "^24.0.14",
    "@types/react": "^19.1.8",
    "@types/react-dom": "^19.1.6",
    "@vitejs/plugin-react": "^4.6.0",
    "eslint": "^9.30.1",
    "eslint-plugin-react-hooks": "^5.2.0",
    "eslint-plugin-react-refresh": "^0.4.20",
    "globals": "^16.3.0",
    "tw-animate-css": "^1.3.5",
    "typescript": "~5.8.3",
    "typescript-eslint": "^8.35.1",
    "vite": "^7.0.4"
  }
}
```

### Output Format

You shall output the full content of all changed source files. Each source file starts with the header line: @@ {file-path} @@, e.g. @@ src/App.tsx @@, and followed by the file content.
*  The response must **only** contain the source code. You **must not** include conversational text, such as greetings, explanations.
*  **Do not** add markdown code fences around code, such as \`\`\`tsx  ... \`\`\`
*  **Do not** include `src/api.ts` in the response
*  Prefix the file path with `--` to indicate removal. You shall omit content in this case.

**Example:**
```
@@ src/App.tsx @@
import React, { useState, useEffect } from 'react';
import { Todo, getTodos, addTodo, updateTodo, deleteTodo } from './api';
import AddTodoForm from './components/AddTodoForm';
import TodoList from './components/TodoList';
import { Loader2 } from 'lucide-react';
import { Toaster } from "./components/ui/sonner";
import { toast } from 'sonner';
import { Button } from './components/ui/button';

const App: React.FC = () => {
  const [todos, setTodos] = useState<Todo[]>([]);
  const [loading, setLoading] = useState(true);
  const [isAdding, setIsAdding] = useState(false);
  const [updatingTodos, setUpdatingTodos] = useState<Set<number>>(new Set());
  const [deletingTodos, setDeletingTodos] = useState<Set<number>>(new Set());

  useEffect(() => {
    const fetchTodos = async () => {
      try {
        const fetchedTodos = await getTodos();
        setTodos(fetchedTodos);
      } catch (error) {
        toast.error('获取待办事项列表失败');
      } finally {
        setLoading(false);
      }
    };
    fetchTodos();
  }, []);

  const handleAddTodo = async (text: string) => {
    setIsAdding(true);
    try {
      const newTodo = await addTodo(text);
      setTodos((prevTodos) => [...prevTodos, newTodo]);
      toast.success('待办事项已添加');
    } catch (error) {
      toast.error('添加待办事项失败');
    } finally {
      setIsAdding(false);
    }
  };

  const handleToggleTodo = async (id: number, completed: boolean) => {
    const originalTodos = [...todos];
    setUpdatingTodos((prev) => new Set(prev).add(id));
    setTodos((prevTodos) =>
      prevTodos.map((t) => (t.id === id ? { ...t, completed } : t))
    );

    try {
      await updateTodo(id, { completed });
    } catch (error) {
      toast.error('更新状态失败');
      setTodos(originalTodos);
    } finally {
      setUpdatingTodos((prev) => {
        const newSet = new Set(prev);
        newSet.delete(id);
        return newSet;
      });
    }
  };

  const handleDeleteTodo = async (id: number) => {
    const originalTodos = [...todos];
    setDeletingTodos((prev) => new Set(prev).add(id));
    setTodos((prevTodos) => prevTodos.filter((t) => t.id !== id));

    try {
      await deleteTodo(id);
      toast.success('待办事项已删除');
    } catch (error) {
      toast.error('删除失败');
      setTodos(originalTodos); 
    } finally {
        setDeletingTodos((prev) => {
            const newSet = new Set(prev);
            newSet.delete(id);
            return newSet;
        });
    }
  };

  const handleClearCompleted = () => {
    const completedIds = todos.filter(t => t.completed).map(t => t.id);
    if (completedIds.length === 0) return;

    const originalTodos = [...todos];
    setTodos(todos.filter(t => !t.completed));
    
    // This is a simplified example of a bulk delete.
    // In a real app, you might have a dedicated API endpoint.
    Promise.all(completedIds.map(id => deleteTodo(id)))
        .then(() => {
            toast.success('已清除所有已完成的待办事项');
        })
        .catch(() => {
            toast.error('清除已完成事项失败');
            setTodos(originalTodos);
        });
  };

  const hasCompleted = todos.some(t => t.completed);

  return (
    <>
      <main className="container mx-auto p-4 max-w-2xl">
        <header className="text-center my-8">
          <h1 className="text-4xl font-bold tracking-tight">待办事项</h1>
          <p className="text-muted-foreground mt-2">使用 React 和 shadcn/ui 构建</p>
        </header>

        <div className="w-full">
          <AddTodoForm onAdd={handleAddTodo} isAdding={isAdding} />

          {loading ? (
            <div className="flex justify-center items-center mt-8">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
          ) : (
            <>
              <TodoList
                todos={todos}
                onToggle={handleToggleTodo}
                onDelete={handleDeleteTodo}
                updatingTodos={updatingTodos}
                deletingTodos={deletingTodos}
              />
              {todos.length > 0 && (
                <div className="mt-4 flex justify-end">
                  <Button
                    variant="ghost"
                    onClick={handleClearCompleted}
                    disabled={!hasCompleted}
                  >
                    清除已完成
                  </Button>
                </div>
              )}
            </>
          )}
        </div>
      </main>
      <Toaster richColors position="top-right" />
    </>
  );
};

export default App;
@@ --src/components/TodoStats.tsx @@
```

### Constraint

*   **DO NOT** output any conversational text, explanations, apologies, or markdown code blocks
*   All changes must be contained within the `src/App.tsx` file.
*   Do not use `useCallback` hook.
*   The web page must adapt to both computer and mobile phone screens. 
*   `useNavigate` **MUST** be used inside the context of a <Router> component.
*   Ensure proper spacing between essential components, particularly between labels and form inputs.