You are an expert AI frontend developer.

Your goal is to generate a modern and **fully-functional** single-page application based on the provided details.

### Application Description

{}

### Backend API (`src/api.ts`)

{}

### Output Format

You shall output the full content of all created source files. Each source file starts with the header line: @@ {file-path} @@, e.g. @@ src/App.tsx @@, and followed by the file content.
*  The following files are provided, **do not** include them in the response:
    *   `src/main.tsx`
    *   `src/api.ts`
    *   `package.json`
    *   `vite.config.ts`
    *   `tsconfig.json`
    *   `tsconfig.app.json`
    *   `tsconfig.node.json`
    *   `index.html`
    *   `src/index.css` (Tailwind CSS setup)
    *   `src/lib/utils.ts` (cn helper function)
    *   All component files under `src/components/ui/` (e.g. `button.tsx`, `card.tsx`, etc.)
*  The response must **only** contain the source code. You **must not** include conversational text, such as greetings, explanations.
*  **Do not** add markdown code fences around code, such as \`\`\`tsx  ... \`\`\`

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
            <TodoList
              todos={todos}
              onToggle={handleToggleTodo}
              onDelete={handleDeleteTodo}
              updatingTodos={updatingTodos}
              deletingTodos={deletingTodos}
            />
          )}
        </div>
      </main>
      <Toaster richColors position="top-right" />
    </>
  );
};

export default App;
@@ src/components/AddTodoForm.tsx @@
import React from 'react';
import { useForm } from 'react-hook-form';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Form, FormControl, FormField, FormItem, FormMessage } from './ui/form';
import { Loader2 } from 'lucide-react';

interface AddTodoFormProps {
  onAdd: (text: string) => Promise<void>;
  isAdding: boolean;
}

type FormValues = {
  text: string;
};

const AddTodoForm: React.FC<AddTodoFormProps> = ({ onAdd, isAdding }) => {
  const form = useForm<FormValues>({
    defaultValues: {
      text: '',
    },
  });

  const onSubmit = async (values: FormValues) => {
    await onAdd(values.text);
    form.reset();
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="flex w-full items-start space-x-2">
        <FormField
          control={form.control}
          name="text"
          rules={{ required: '待办事项不能为空。' }}
          render={({ field }) => (
            <FormItem className="flex-1">
              <FormControl>
                <Input placeholder="添加一个新的待办事项..." {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <Button type="submit" disabled={isAdding} className="w-24">
          {isAdding ? <Loader2 className="h-4 w-4 animate-spin" /> : '添加'}
        </Button>
      </form>
    </Form>
  );
};

export default AddTodoForm;
@@ src/components/TodoList.tsx @@
import React from 'react';
import { Todo } from '../api';
import { Button } from './ui/button';
import { Checkbox } from './ui/checkbox';
import { Card, CardContent } from './ui/card';
import { Trash2, Loader2 } from 'lucide-react';
import { cn } from '../lib/utils';

interface TodoListProps {
  todos: Todo[];
  onToggle: (id: number, completed: boolean) => void;
  onDelete: (id: number) => void;
  updatingTodos: Set<number>;
  deletingTodos: Set<number>;
}

const TodoList: React.FC<TodoListProps> = ({ todos, onToggle, onDelete, updatingTodos, deletingTodos }) => {
  if (todos.length === 0) {
    return (
      <div className="text-center text-muted-foreground mt-8 py-10 border-dashed border-2 rounded-lg">
        <p>太棒了！没有待办事项。</p>
      </div>
    );
  }

  return (
    <Card className="mt-4 border-none shadow-none">
      <CardContent className="p-0">
        <ul className="divide-y rounded-lg border">
          {todos.map((todo) => (
            <li key={todo.id} className="flex items-center p-4">
              <Checkbox
                id={`todo-${todo.id}`}
                checked={todo.completed}
                onCheckedChange={(checked) => onToggle(todo.id, !!checked)}
                disabled={updatingTodos.has(todo.id) || deletingTodos.has(todo.id)}
                className="mr-4"
              />
              <label
                htmlFor={`todo-${todo.id}`}
                className={cn(
                  'flex-1 text-sm font-medium leading-none transition-colors cursor-pointer',
                  todo.completed ? 'line-through text-muted-foreground' : '',
                  (updatingTodos.has(todo.id) || deletingTodos.has(todo.id)) ? 'opacity-50' : ''
                )}
              >
                {todo.text}
              </label>
              <Button
                variant="ghost"
                size="icon"
                onClick={() => onDelete(todo.id)}
                disabled={deletingTodos.has(todo.id) || updatingTodos.has(todo.id)}
                aria-label="删除待办事项"
              >
                {deletingTodos.has(todo.id) ? (
                   <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Trash2 className="h-4 w-4 text-muted-foreground hover:text-destructive" />
                )}
              </Button>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  );
};

export default TodoList;
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

### Requirements

1. Use React, TypeScript, and Ant Design. No other libraries.
2. Import `src/api.ts`.
3. All UI text must be in **Chinese**.
4. The application must be fully functional.
5. Do not use `useCallback` hook. 
6. The web page must adapt to both computer and mobile phone screens. 
7. `useNavigate` **MUST** be used inside the context of a <Router> component.
8. Ensure proper spacing between essential components, particularly between labels and form inputs.
