The generated code didn't compile. You need to modify the code according to the build error. 

Here is the build error:

{}

### Output format

You shall output the full content of all changed source files. Each source file starts with the header line: @@ {file-path} @@, e.g. @@ src/App.tsx @@, and followed by the file content.
*  The response must **only** contain the source code. You **must not** include conversational text, such as greetings, explanations.
*  **Do not** add markdown code fences around code, such as \`\`\`tsx  ... \`\`\`
*  **Do not** include `src/api.ts` in the response
*  Prefix the file path with `--` to indicate removal. You shall omit content in this case.

**Example:**
```
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
@@ --src/components/TodoModal.tsx @@
```