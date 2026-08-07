INSERT INTO role (id, code, description) VALUES
    (gen_random_uuid(), 'SUPER_ADMIN', 'Gerencia toda a plataforma, usuários e cursos.'),
    (gen_random_uuid(), 'INSTRUCTOR', 'Cria e gerencia seus próprios cursos, módulos e aulas.'),
    (gen_random_uuid(), 'STUDENT', 'Consome cursos nos quais está matriculado.');
