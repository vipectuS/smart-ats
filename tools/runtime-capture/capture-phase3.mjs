import fs from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import { Builder, By, until } from 'selenium-webdriver';
import firefox from 'selenium-webdriver/firefox.js';

const frontendBaseUrl = process.env.FRONTEND_BASE_URL || 'http://127.0.0.1:4173';
const assetBaseUrl = process.env.ASSET_BASE_URL || 'http://127.0.0.1:8088';
const outputDir = process.env.CAPTURE_OUTPUT_DIR || path.resolve(process.cwd(), '../../doc/synthetic-dataset/screenshots');
const pdfPath = process.env.PDF_SAMPLE_PATH || path.resolve(process.cwd(), '../../doc/synthetic-dataset/pdf/C01_resume_sample.pdf');
const candidateUsername = process.env.CANDIDATE_USERNAME || 'candemo01';
const candidatePassword = process.env.CANDIDATE_PASSWORD || 'Candemo123';
const candidateEmail = process.env.CANDIDATE_EMAIL || 'candemo01@example.com';
const firefoxExecutablePath = process.env.FIREFOX_EXECUTABLE_PATH || '/usr/bin/firefox-esr';

const githubUrl = `${assetBaseUrl}/github_C01_linzeyuan-example.html`;
const portfolioUrl = `${assetBaseUrl}/portfolio_C01_%E6%9E%97%E6%B3%BD%E8%BF%9C.html`;

async function ensureDir(dir) {
  await fs.mkdir(dir, { recursive: true });
}

function buttonByText(text) {
  return By.xpath(`//button[contains(normalize-space(.), "${text}")]`);
}

function textLocator(text) {
  return By.xpath(`//*[contains(normalize-space(.), "${text}")]`);
}

async function setCaptureWindow(driver, minimumHeight = 1100) {
  const pageHeight = await driver.executeScript(`
    return Math.max(
      document.body.scrollHeight,
      document.documentElement.scrollHeight,
      document.body.offsetHeight,
      document.documentElement.offsetHeight,
      window.innerHeight
    );
  `);
  const height = Math.max(minimumHeight, Math.min(Number(pageHeight || minimumHeight) + 120, 4200));
  await driver.manage().window().setRect({ width: 1440, height });
  await driver.sleep(500);
}

async function saveScreenshot(driver, fileName, minimumHeight) {
  await setCaptureWindow(driver, minimumHeight);
  const image = await driver.takeScreenshot();
  await fs.writeFile(path.join(outputDir, fileName), image, 'base64');
}

async function clearAndType(driver, locator, value) {
  const element = await driver.wait(until.elementLocated(locator), 30000);
  await driver.wait(until.elementIsVisible(element), 30000);
  await element.clear();
  await element.sendKeys(value);
}

async function clickWhenReady(driver, locator) {
  const element = await driver.wait(until.elementLocated(locator), 30000);
  await driver.wait(until.elementIsVisible(element), 30000);
  await driver.wait(until.elementIsEnabled(element), 30000);
  await element.click();
}

async function waitForText(driver, text, timeout = 30000) {
  await driver.wait(until.elementLocated(textLocator(text)), timeout);
}

async function captureStaticPages(driver) {
  await driver.get(portfolioUrl);
  await waitForText(driver, '林泽远');
  await saveScreenshot(driver, 'phase3_input_portfolio_page.png', 1600);

  await driver.get(githubUrl);
  await waitForText(driver, 'smart-ats-backend-lab');
  await saveScreenshot(driver, 'phase3_input_github_page.png', 1500);
}

async function registerCandidate(driver) {
  await driver.get(`${frontendBaseUrl}/register`);
  await clearAndType(driver, By.id('username'), candidateUsername);
  await clearAndType(driver, By.id('email'), candidateEmail);
  await clearAndType(driver, By.id('password'), candidatePassword);
  await clickWhenReady(driver, buttonByText('注册账号'));
  await driver.wait(until.urlContains('/login'), 30000);
}

async function loginCandidate(driver) {
  await driver.get(`${frontendBaseUrl}/login`);
  await clearAndType(driver, By.id('username'), candidateUsername);
  await clearAndType(driver, By.id('password'), candidatePassword);
  await clickWhenReady(driver, buttonByText('登录'));
  await driver.wait(until.urlContains('/candidate/dashboard'), 30000);
}

async function captureProfileAndUpload(driver) {
  await driver.get(`${frontendBaseUrl}/candidate/profile`);
  await clearAndType(driver, By.id('candidate-github'), githubUrl);
  await clearAndType(driver, By.id('candidate-portfolio'), portfolioUrl);
  await saveScreenshot(driver, 'phase3_input_profile_urls.png', 1800);

  await clickWhenReady(driver, buttonByText('保存资料'));
  await waitForText(driver, '候选人资料已更新', 30000);

  const fileInput = await driver.wait(until.elementLocated(By.id('candidate-resume-file')), 30000);
  await fileInput.sendKeys(pdfPath);

  await clickWhenReady(driver, buttonByText('执行浏览器端渲染'));
  await waitForText(driver, '预处理结果已生成', 120000);
  await saveScreenshot(driver, 'phase3_input_pdf_preprocess.png', 2400);

  await clickWhenReady(driver, buttonByText('提交预处理结果'));
  await waitForText(driver, '候选人简历已上传', 30000);
}

async function waitForParsedState(driver) {
  for (let attempt = 0; attempt < 50; attempt += 1) {
    await driver.get(`${frontendBaseUrl}/candidate/profile`);
    const content = await driver.findElement(By.tagName('body')).getText();
    if (content.includes('解析完成')) {
      return;
    }
    await driver.sleep(2000);
  }
  throw new Error('Timed out waiting for parsed resume state');
}

async function captureOutputs(driver) {
  await saveScreenshot(driver, 'phase3_output_resume_parsed.png', 2200);

  const summary = await driver.wait(until.elementLocated(textLocator('画像摘要')), 30000);
  await driver.executeScript('arguments[0].scrollIntoView({block: "center"});', summary);
  await driver.sleep(600);
  await saveScreenshot(driver, 'phase3_output_profile_summary.png', 1800);

  await driver.get(`${frontendBaseUrl}/candidate/dashboard`);
  await waitForText(driver, '推荐职位', 30000);
  await driver.sleep(1500);
  await saveScreenshot(driver, 'phase3_output_job_fit.png', 2200);
}

async function main() {
  await ensureDir(outputDir);

  const options = new firefox.Options()
    .addArguments('-headless')
    .setBinary(firefoxExecutablePath)
    .windowSize({ width: 1440, height: 1200 });

  const driver = await new Builder()
    .forBrowser('firefox')
    .setFirefoxOptions(options)
    .build();

  try {
    await captureStaticPages(driver);
    await registerCandidate(driver);
    await loginCandidate(driver);
    await captureProfileAndUpload(driver);
    await waitForParsedState(driver);
    await captureOutputs(driver);
  } finally {
    await driver.quit();
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});