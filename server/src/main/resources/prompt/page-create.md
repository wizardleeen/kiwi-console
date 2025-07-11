You are an expert AI frontend developer.

Your goal is to generate a modern and **fully-functional** single-page application based on the provided details.

### Application Description

{}

### Backend API (`src/api.ts`)

{}

### Output Format

You shall output the full content of all created source files. Each source file starts with the header line: @@ {file-path} @@, e.g. @@ src/App.tsx @@, and followed by the file content.
*  The following files are provided, **do not** include them in the response: 
  * `src/main.tsx`
  * `src/api.ts`
  * `package.json`
  * `vite.config.ts`
  * `tsconfig.json`
  * `tsconfig.app.json`
  * `tsconfig.node.json`
  * `index.html`
*  The response must **only** contain the source code. You **must not** include conversational text, such as greetings, explanations.   
*  **Do not** add markdown code fences around code, such as \`\`\`tsx  ... \`\`\`

**Example:**
```
@@ src/App.tsx @@
import React, { useState, useEffect } from 'react';
import { Layout, Typography, Row, Col, message, Spin, ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { Todo, getTodos, addTodo, updateTodo, deleteTodo } from './api';
import TodoList from './components/TodoList';
import AddTodoForm from './components/AddTodoForm';
import './index.css';

const { Header, Content } = Layout;
const { Title } = Typography;

const App: React.FC = () => {
const [todos, setTodos] = useState<Todo[]>([]);
const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchTodos = async () => {
            try {
                const fetchedTodos = await getTodos();
                setTodos(fetchedTodos);
            } catch (error) {
                message.error('获取待办事项失败');
            } finally {
                setLoading(false);
            }
        };
        fetchTodos();
    }, []);

    const handleAddTodo = async (text: string) => {
        try {
            const newTodo = await addTodo(text);
            setTodos([...todos, newTodo]);
            message.success('添加成功');
        } catch (error) {
            message.error('添加待办事项失败');
        }
    };

    const handleToggleTodo = async (id: number) => {
        const todo = todos.find(t => t.id === id);
        if (todo) {
            try {
                const updated = await updateTodo(id, { completed: !todo.completed });
                setTodos(todos.map(t => (t.id === id ? updated : t)));
            } catch (error) {
                message.error('更新状态失败');
            }
        }
    };

    const handleDeleteTodo = async (id: number) => {
        try {
            await deleteTodo(id);
            setTodos(todos.filter(t => t.id !== id));
            message.success('删除成功');
        } catch (error) {
            message.error('删除失败');
        }
    };

    return (
        <ConfigProvider locale={zhCN}>
            <Layout style={{ minHeight: '100vh' }}>
                <Header style={{ display: 'flex', alignItems: 'center' }}>
                    <Title level={3} style={{ color: 'white', margin: 0 }}>待办事项应用</Title>
                </Header>
                <Content style={{ padding: '0 24px', marginTop: 24 }}>
                    <Row justify="center">
                        <Col xs={24} sm={20} md={16} lg={12} xl={8}>
                            <div className="site-layout-content">
                                <AddTodoForm onAdd={handleAddTodo} />
                                {loading ? (
                                    <div style={{ textAlign: 'center', marginTop: '20px' }}>
                                        <Spin size="large" />
                                    </div>
                                ) : (
                                    <TodoList
                                        todos={todos}
                                        onToggle={handleToggleTodo}
                                        onDelete={handleDeleteTodo}
                                    />
                                )}
                            </div>
                        </Col>
                    </Row>
                </Content>
            </Layout>
        </ConfigProvider>
    );
};

export default App;
@@ src/components/TodoList.tsx @@
import React from 'react';
import { List, Checkbox, Button, Typography, Empty } from 'antd';
import { DeleteOutlined } from '@ant-design/icons';
import { Todo } from '../api';

interface TodoListProps {
todos: Todo[];
onToggle: (id: number) => void;
onDelete: (id: number) => void;
}

const TodoList: React.FC<TodoListProps> = ({ todos, onToggle, onDelete }) => {
if (todos.length === 0) {
return <Empty description="暂无待办事项" style={{ marginTop: 20 }} />;
}

    return (
        <List
            style={{ marginTop: '20px' }}
            bordered
            dataSource={todos}
            renderItem={item => (
                <List.Item
                    actions={[
                        <Button
                            type="text"
                            danger
                            icon={<DeleteOutlined />}
                            onClick={() => onDelete(item.id)}
                        />
                    ]}
                >
                    <Checkbox
                        checked={item.completed}
                        onChange={() => onToggle(item.id)}
                        style={{ marginRight: '10px' }}
                    />
                    <Typography.Text delete={item.completed} style={{ flex: 1 }}>
                        {item.text}
                    </Typography.Text>
                </List.Item>
            )}
        />
    );
};

export default TodoList;
@@ src/components/AddTodoForm.tsx @@
import React from 'react';
import { Form, Input, Button } from 'antd';

interface AddTodoFormProps {
onAdd: (text: string) => void;
}

const AddTodoForm: React.FC<AddTodoFormProps> = ({ onAdd }) => {
const [form] = Form.useForm();

    const onFinish = (values: { text: string }) => {
        onAdd(values.text);
        form.resetFields();
    };

    return (
        <Form
            form={form}
            layout="inline"
            onFinish={onFinish}
            style={{ display: 'flex' }}
        >
            <Form.Item
                name="text"
                rules={[{ required: true, message: '请输入待办事项!' }]}
                style={{ flex: 1, marginRight: '8px' }}
            >
                <Input placeholder="添加新的待办事项" />
            </Form.Item>
            <Form.Item>
                <Button type="primary" htmlType="submit">
                    添加
                </Button>
            </Form.Item>
        </Form>
    );
};

export default AddTodoForm;
@@ src/index.css @@
body {
margin: 0;
font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen',
'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue',
sans-serif;
-webkit-font-smoothing: antialiased;
-moz-osx-font-smoothing: grayscale;
}

.site-layout-content {
background: #fff;
padding: 24px;
border-radius: 8px;
box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
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
    "lint": "eslint . --ext ts,tsx --report-unused-disable-directives --max-warnings 0",
    "preview": "vite preview"
  },
  "dependencies": {
    "@ant-design/icons": "^5.3.7",
    "antd": "^5.17.4",
    "react": "^17.0.2",
    "react-dom": "^17.0.2",
    "react-router-dom": "^6.23.1"
  },
  "devDependencies": {
    "@types/react": "^17.0.80",
    "@types/react-dom": "^17.0.25",
    "@vitejs/plugin-react": "^3.1.0",
    "eslint": "^8.57.0",
    "eslint-plugin-react-hooks": "^4.6.2",
    "eslint-plugin-react-refresh": "^0.4.7",
    "typescript": "~5.4.5",
    "@typescript-eslint/eslint-plugin": "^7.13.0",
    "@typescript-eslint/parser": "^7.13.0",
    "vite": "^4.5.3"
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
