// Licensed under the Apache License, Version 2.0. See LICENSE.txt.
import Community from '@site/hasor-tech/Community';
import React from 'react';
import Layout from '@theme/Layout';
import Heading from '@theme/Heading';
import Link from '@docusaurus/Link';
import CodeBlock from '@theme/CodeBlock';
import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';
import Translate, {translate} from '@docusaurus/Translate';
import useBaseUrl from '@docusaurus/useBaseUrl';
import LicenseBadge from '@site/static/img/labels/license.svg';
import styles from './index.module.css';

const quickStart = '/docs/guides/getting-started/quickstart';
const bootGuide = '/docs/guides/deployment/boot-launcher';
const heroDescription = translate({
    id: 'homepage.hero.description',
    message: '按需组合 IoC、AOP、配置、Web MVC 和启动能力。',
});

const modules = [
    {
        title: 'Core',
        icon: 'layers',
        description: <Translate id="homepage.modules.core">提供可嵌入的容器、IoC、AOP 与生命周期管理。</Translate>,
        action: <Translate id="homepage.modules.core.guide">Core 文档</Translate>,
        path: '/docs/guides/core/overview',
    },
    {
        title: 'Config',
        icon: 'box',
        description: <Translate id="homepage.modules.config">用 @Configuration 和 @Bean 声明并装配对象。</Translate>,
        action: <Translate id="homepage.modules.config.guide">Config 文档</Translate>,
        path: '/docs/guides/core/conf/java-config',
    },
    {
        title: 'Web MVC',
        icon: 'layers',
        description: <Translate id="homepage.modules.web">为 Web 应用提供路由、参数绑定和响应渲染。</Translate>,
        action: <Translate id="homepage.modules.web.guide">Web 文档</Translate>,
        path: '/docs/guides/webmvc/overview',
    },
    {
        title: 'Boot',
        icon: 'circle-play',
        description: <Translate id="homepage.modules.boot">统一启动普通 Java 或内嵌 Web 应用，支持可执行 JAR。</Translate>,
        action: <Translate id="homepage.modules.boot.guide">Boot 文档</Translate>,
        path: bootGuide,
    },
];

const examples = [
    {
        id: 'boot',
        label: <Translate id="homepage.examples.boot">启动应用</Translate>,
        code: `package example;
import net.hasor.boot.Boot;
import net.hasor.boot.BootApplication;
import net.hasor.web.WebApiBinder;
import net.hasor.web.WebModule;

public class Application implements WebModule {
    public static void main(String[] args) throws Exception {
        try (BootApplication app = Boot.run(args, Application.class)) {
            app.join();
        }
    }

    // Serve static files from the classpath under /app/.
    @Override
    public void loadModule(WebApiBinder binder) {
        binder.addClasspathResource("/app/**", "home");
    }
}`,
    },
    {
        id: 'controller',
        label: <Translate id="homepage.examples.controller">Web 控制器</Translate>,
        code: `package example;
import java.util.Collections;
import java.util.Map;
import net.hasor.core.Inject;
import net.hasor.web.annotation.Get;
import net.hasor.web.annotation.MappingTo;

@MappingTo("/hello")
public class HelloController {
    @Inject
    private HelloService service;

    @Get
    public Map<String, String> hello() {
        return Collections.singletonMap("message", service.greet());
    }
}`,
    },
    {
        id: 'service',
        label: <Translate id="homepage.examples.service">业务服务</Translate>,
        code: `package example;
import net.hasor.config.Bean;

@Bean
public class HelloService {
    public String greet() {
        return "Hello, Hasor!";
    }
}`,
    },
];

function Icon({name}) {
    const source = useBaseUrl('/img/home/icons/' + name + '.svg');
    return <img className={styles.icon} src={source} width="24" height="24" alt="" />;
}

function Feature({icon, title, description}) {
    return <div className={styles.feature}>
        <Icon name={icon} />
        <div><strong>{title}</strong><span>{description}</span></div>
    </div>;
}

const configuration = <Feature icon="settings"
    title={<Translate id="homepage.architecture.config">配置（可选）</Translate>}
    description={<Translate id="homepage.architecture.config.description">注解配置与模块声明</Translate>} />;

function Architecture() {
    return <figure className={styles.architecture} role="img" aria-label={translate({
        id: 'homepage.hero.illustration',
        message: '普通 Java 应用和 Web 应用共用 hasor-core，提供依赖注入、AOP 和生命周期管理。可按需加入注解配置和 Boot，Web MVC 提供路由与参数绑定。',
    })}>
        <div className={styles.paths}>
            <div className={styles.appPath}>
                <div className={styles.appTitle}><Icon name="coffee" />
                    <Translate id="homepage.architecture.java">普通 Java 应用</Translate>
                </div>
                <div className={styles.pathFeatures}>
                    {configuration}
                    <Feature icon="circle-play"
                        title={<Translate id="homepage.architecture.boot">启动（可选）</Translate>}
                        description={<Translate id="homepage.architecture.boot.description">统一启动与打包</Translate>} />
                </div>
            </div>
            <div className={styles.appPath}>
                <div className={styles.appTitle}><Icon name="layers" />
                    <Translate id="homepage.architecture.web">Web 应用</Translate>
                </div>
                <div className={styles.pathFeatures}>
                    {configuration}
                    <Feature icon="layers" title="Web MVC"
                        description={<Translate id="homepage.architecture.web.description">路由与参数绑定</Translate>} />
                </div>
            </div>
        </div>
        <div className={styles.core}>
            <strong><Icon name="layers" />hasor-core <Translate id="homepage.architecture.core">（应用基础）</Translate></strong>
            <ul>
                <li><Translate id="homepage.modules.ioc">IoC 依赖注入</Translate></li>
                <li><Translate id="homepage.modules.aop">AOP 方法拦截</Translate></li>
                <li><Translate id="homepage.architecture.lifecycle">生命周期</Translate></li>
            </ul>
        </div>
    </figure>;
}

function Hero() {
    return <header className={styles.hero}>
        <div className="container">
            <h1><Translate id="homepage.hero.title">一个 Java 应用开发框架</Translate></h1>
            <p className={styles.heroDescription}>{heroDescription}</p>
            <div className={styles.actions}>
                <Link className={styles.primaryButton} to={quickStart}>
                    <Translate id="homepage.quickstart">快速开始</Translate><span aria-hidden="true">→</span>
                </Link>
                <Link className={styles.secondaryButton} to="/docs/guides/getting-started/overview">
                    <Translate id="homepage.hero.overview">认识 Hasor</Translate>
                </Link>
            </div>
            <Architecture />
        </div>
    </header>;
}

function Modules() {
    return <section className={styles.modules} aria-labelledby="modules">
        <div className="container">
            <Heading as="h2" id="modules"><Translate id="homepage.modules.title">按需组合</Translate></Heading>
            <p className={styles.sectionDescription}>
                <Translate id="homepage.modules.description">从 hasor-core 开始，按需加入配置、Web MVC 和启动模块。</Translate>
            </p>
            <ul className={styles.moduleGrid}>
                {modules.map(module => <li className={styles.moduleCard} key={module.title}>
                    <Icon name={module.icon} />
                    <h3>{module.title}</h3>
                    <p>{module.description}</p>
                    <Link to={module.path}>{module.action} <span aria-hidden="true">→</span></Link>
                </li>)}
            </ul>
        </div>
    </section>;
}

function Examples() {
    return <section className={styles.examples} aria-labelledby="hasor-examples-title">
        <div className={`container ${styles.examplesLayout}`}>
            <div>
                <h2 id="hasor-examples-title"><Translate id="homepage.examples.title">开发示例</Translate></h2>
                <div className={`${styles.sectionDescription} ${styles.exampleInstructions}`}>
                    <p><Translate id="homepage.examples.description">启动后</Translate></p>
                    <ul>
                        <li><Translate id="homepage.examples.endpoint" values={{path: <code>/hello</code>}}>
                            {'访问 {path} 获取 JSON'}
                        </Translate></li>
                        <li><Translate id="homepage.examples.staticPage" values={{path: <code>/app/</code>}}>
                            {'访问 {path} 打开静态首页。'}
                        </Translate></li>
                    </ul>
                </div>
                <Link className={styles.primaryButton} to="/docs/guides/deployment/web-launcher">
                    <Translate id="homepage.examples.guide">Web 启动器</Translate>
                </Link>
            </div>
            <div className={styles.exampleTabs}>
                <Tabs defaultValue="boot">
                    {examples.map(example => <TabItem key={example.id} value={example.id} label={example.label}>
                        <CodeBlock language="java">{example.code}</CodeBlock>
                    </TabItem>)}
                </Tabs>
            </div>
        </div>
    </section>;
}

function Participate() {
    return <section className={styles.participation} aria-labelledby="hasor-participation-title">
        <div className="container">
            <Community>
                <div className={styles.participationHeading}>
                    <h2 id="hasor-participation-title"><Translate id="homepage.participation.title">加入社区</Translate></h2>
                    <a className={styles.licenseBadge} href="https://www.apache.org/licenses/LICENSE-2.0.html"
                       target="_blank" rel="noopener noreferrer" aria-label="Apache License 2.0">
                        <LicenseBadge aria-hidden="true" focusable="false"/>
                    </a>
                </div>
                <p><Translate id="homepage.participation.description">交流使用经验、反馈问题，参与 Hasor 的改进。</Translate></p>
            </Community>
        </div>
    </section>;
}

export default function Home() {
    return <Layout title={translate({id: 'homepage.title', message: '模块化 Java 应用框架'})}
        description={heroDescription}>
        <main className={styles.home}>
            <Hero />
            <Modules />
            <Examples />
            <Participate />
        </main>
    </Layout>;
}
