import React from 'react';
import clsx from 'clsx';
import Layout from '@theme/Layout';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import useBaseUrl from '@docusaurus/useBaseUrl';
import styles from './index.module.css';
import HomepageFeatures from '../components/HomepageFeatures';

function HomepageHeader() {
  const {siteConfig} = useDocusaurusContext();
  return (
    <header className={clsx('hero hero--primary', styles.heroBanner)}>
      <div className="container">
        <h1 className="hero__title">
          Scala Advent of Code by
          <a href="https://scala.epfl.ch/">
            <img className={styles.scalacenter} alt="Scala Center" src={useBaseUrl('/img/scala-center.png')} title="Scala Center"/>
          </a>
        </h1>
        <p className="hero__subtitle">{siteConfig.tagline}</p>
        <div className={styles.buttons}>
          <Link
            className={`${styles.button} button button--secondary button--lg`}
            to="/puzzles/day1">
            Day 1
          </Link>
          <Link
              className={`${styles.button} button button--primary button--lg`}
              to="/puzzles/day2">
            Day 2
          </Link>
          <Link
              className={`${styles.button} button button--error button--lg`}
              disabled
              to="/puzzles/day3">
            Day 3
          </Link>
          <Link
              className={`${styles.button} button button--error button--lg`}
              disabled
              to="/puzzles/day4">
            Day 4
          </Link>
          <Link
              className={`${styles.button} button button--error button--lg`}
              disabled
              to="/puzzles/day5">
            Day 5
          </Link>
        </div>
      </div>
    </header>
  );
}

export default function Home() {
  const {siteConfig} = useDocusaurusContext();
  return (
    <Layout
      title={siteConfig.title}
      description="Scala advent of code by the scala center">
      <HomepageHeader />
      <main>
        <HomepageFeatures />
      </main>
    </Layout>
  );
}
